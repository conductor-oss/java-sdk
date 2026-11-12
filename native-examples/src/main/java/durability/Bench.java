/*
 * Copyright 2026 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package durability;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import durability.LedgerModel.LedgerInput;
import durability.LedgerModel.LedgerResult;
import durability.Processes.Dice;
import durability.Processes.Workers.Slot;

/**
 * A Jepsen-style failure bench for native workflows. It runs the {@link LedgerWorkflow} workload on
 * several worker processes while a nemesis kills, pauses and partitions the workers and the server,
 * records what every client and worker did, and then checks the histories with {@link Checker}.
 *
 * <p>Phases, as in Jepsen:
 *
 * <ol>
 *   <li><b>main</b>: start executions (idempotent, retried on failure), send signals and updates to
 *       running ones, and inject one fault at a time;
 *   <li><b>recovery</b>: heal everything, let in-flight updates finish, send {@code close} to every
 *       execution, and wait for all of them to reach a terminal status;
 *   <li><b>final</b>: read every execution from the server and run the checker.
 * </ol>
 *
 * Configuration, all from the environment:
 *
 * <pre>
 *   BENCH_SERVER_PORT     server under test (default 8099; 8080 is refused)
 *   BENCH_SERVER_SCRIPT   script that runs the server in the foreground; needed for server faults.
 *                         In cluster mode it is called with two arguments: HTTP port, database port
 *   BENCH_CLUSTER         comma list of server ports, e.g. 8101,8102,8103 (cluster mode); each
 *                         server reaches the database through its own partitionable proxy
 *   BENCH_DB_PORT         the database's port (default 26432); BENCH_DB_PROXY_BASE + i + 1 is
 *                         server i's proxy to it (default base 26440)
 *   BENCH_DB_CONTAINER    the database's Docker container, for db-pause and db-kill
 *   BENCH_CLIENT_PORT     the clients' load balancer across the cluster (default 18098; never partitioned)
 *   BENCH_PROXY_PORT      port of the partitioning proxy the workers use (default 18097)
 *   BENCH_WORKERS         worker processes (default 3)
 *   BENCH_WORKFLOWS       executions to start (default 30)
 *   BENCH_MAIN_SECONDS    length of the main phase (default 180)
 *   BENCH_RECOVERY_SECONDS  how long executions get to finish after healing (default 600)
 *   BENCH_FAULTS          comma list of worker-kill, worker-pause, server-kill, server-pause,
 *                         partition, and in cluster mode db-pause, db-kill, db-partition,
 *                         cluster-kill; or none (default: the first five, plus the cluster ones
 *                         in cluster mode)
 *   BENCH_FAULT_INTERVAL  mean seconds between faults (default 12)
 *   BENCH_NEMESES         independent fault injectors, so faults can overlap (default 1)
 *   BENCH_SIGNAL_MS       interval between signals (default 250)
 *   BENCH_UPDATE_MS       interval between updates, per updater (default 1500)
 *   BENCH_SIGNAL_ON_START send a deposit right after each start, into the first continuation (default true)
 *   BENCH_SEED            seed for the fault schedule and step sleeps (default: time)
 *   BENCH_OUT             output directory (default build/durability/&lt;timestamp&gt;)
 * </pre>
 */
public class Bench {

    static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final int serverPort = intEnv("BENCH_SERVER_PORT", 8099);
    private final List<Integer> clusterPorts = Arrays.stream(env("BENCH_CLUSTER", "").split(","))
            .map(String::trim).filter(x -> !x.isEmpty()).map(Integer::parseInt).toList();
    private final int dbPort = intEnv("BENCH_DB_PORT", 26432);
    private final int dbProxyBase = intEnv("BENCH_DB_PROXY_BASE", 26440);
    private final int clientPort = intEnv("BENCH_CLIENT_PORT", 18098);
    private final int proxyPort = intEnv("BENCH_PROXY_PORT", 18097);
    private final int workerCount = intEnv("BENCH_WORKERS", 3);
    private final int workflows = intEnv("BENCH_WORKFLOWS", 30);
    private final int mainSeconds = intEnv("BENCH_MAIN_SECONDS", 180);
    private final int recoverySeconds = intEnv("BENCH_RECOVERY_SECONDS", 600);
    private final double faultInterval = intEnv("BENCH_FAULT_INTERVAL", 12);
    private final int nemeses = intEnv("BENCH_NEMESES", 1);
    private final int signalMs = intEnv("BENCH_SIGNAL_MS", 250);
    private final int updateMs = intEnv("BENCH_UPDATE_MS", 1500);
    private final boolean signalOnStart = !"false".equals(env("BENCH_SIGNAL_ON_START", "true"));
    private final long seed = Long.parseLong(env("BENCH_SEED", Long.toString(System.currentTimeMillis())));
    private final List<String> faults = Arrays.stream(
                    env("BENCH_FAULTS", "worker-kill,worker-pause,server-kill,server-pause,partition"
                            + (System.getenv("BENCH_CLUSTER") == null ? "" : ",db-pause,db-kill,db-partition,cluster-kill"))
                            .split(","))
            .map(String::trim).filter(f -> !f.isEmpty() && !f.equals("none")).toList();
    private final String runId = "bench-" + Long.toString(System.currentTimeMillis(), 36);

    private final Path out;
    private final History history;
    private final Dice dice = new Dice(seed);
    private final List<Processes.Server> servers = new ArrayList<>();
    private final List<Proxy> dbProxies = new ArrayList<>();
    private Processes.Database database;
    private Proxy clientProxy;
    private final Processes.Workers workers;
    private final Proxy proxy;
    private final Conductor conductor;
    private final Processes.Api api;
    private final Nemesis nemesis;

    /** Idempotency key to the execution id the server returned for it. */
    private final Map<String, String> ids = new ConcurrentHashMap<>();
    private final Set<String> pendingStarts = ConcurrentHashMap.newKeySet();
    /** Executions seen in a terminal status during recovery. */
    private final Set<String> finished = ConcurrentHashMap.newKeySet();
    private final AtomicInteger sequence = new AtomicInteger();
    private final AtomicBoolean mainPhase = new AtomicBoolean(true);

    Bench() throws Exception {
        if (serverPort == 8080 || proxyPort == 8080 || clusterPorts.contains(8080) || clientPort == 8080) {
            throw new IllegalArgumentException("refusing to use port 8080");
        }
        out = Path.of(env("BENCH_OUT", "build/durability/"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))));
        Files.createDirectories(out.resolve("ledger"));
        history = new History(out.resolve("history.jsonl"));
        String script = System.getenv("BENCH_SERVER_SCRIPT");
        String base;
        if (clusterPorts.isEmpty()) {
            servers.add(new Processes.Server(serverPort, script, out));
            proxy = new Proxy(proxyPort, "127.0.0.1", serverPort);
            base = "http://localhost:" + serverPort + "/api";
        } else {
            for (int i = 0; i < clusterPorts.size(); i++) {
                int serverDbPort = dbProxyBase + i + 1;
                dbProxies.add(new Proxy(serverDbPort, "127.0.0.1", dbPort));
                servers.add(new Processes.Server(clusterPorts.get(i), script, out,
                        String.valueOf(clusterPorts.get(i)), String.valueOf(serverDbPort)));
            }
            database = new Processes.Database(env("BENCH_DB_CONTAINER", "native-bench-pg"), dbPort);
            proxy = new Proxy(proxyPort, "127.0.0.1", clusterPorts);
            clientProxy = new Proxy(clientPort, "127.0.0.1", clusterPorts);
            base = "http://127.0.0.1:" + clientPort + "/api";
        }
        Map<String, String> workerEnv = new LinkedHashMap<>();
        System.getenv().forEach((k, v) -> {
            if (k.startsWith("BENCH_STEP_")) {
                workerEnv.put(k, v);
            }
        });
        workers = new Processes.Workers(workerCount, "http://127.0.0.1:" + proxyPort + "/api", out, workerEnv);
        conductor = Conductor.of(ConductorClient.builder().basePath(base)
                .connectTimeout(3_000).readTimeout(15_000).writeTimeout(15_000).build());
        api = new Processes.Api(base);
        nemesis = new Nemesis(faults, faultInterval, dice, workers, servers, proxy, dbProxies, database, history::nemesis);
    }

    public static void main(String[] args) throws Exception {
        Bench bench = new Bench();
        int status = bench.run();
        System.exit(status);
    }

    int run() throws Exception {
        Map<String, Object> config = Processes.mapOf(
                "runId", runId, "serverPort", serverPort, "cluster", clusterPorts, "proxyPort", proxyPort,
                "workers", workerCount,
                "workflows", workflows, "mainSeconds", mainSeconds, "recoverySeconds", recoverySeconds,
                "faults", faults, "faultInterval", faultInterval, "nemeses", nemeses, "signalMs", signalMs, "updateMs", updateMs,
                "seed", seed);
        Files.writeString(out.resolve("config.json"), JSON.writeValueAsString(config));
        log("run " + runId + " -> " + out.toAbsolutePath());
        log("config " + config);

        if (database != null) {
            database.ensureUp(Duration.ofMinutes(2));
        }
        // one at a time: the first server runs the schema migrations alone
        for (Processes.Server s : servers) {
            s.ensureUp(Duration.ofMinutes(4));
        }
        workers.startAll();
        waitForDefinition();

        long mainEnd = System.currentTimeMillis() + mainSeconds * 1000L;
        List<Thread> clients = new ArrayList<>();
        clients.add(Thread.ofVirtual().name("starter").start(() -> starter(mainEnd)));
        clients.add(Thread.ofVirtual().name("signaler").start(() -> signaler(mainEnd)));
        for (int i = 0; i < 3; i++) {
            int n = i;
            clients.add(Thread.ofVirtual().name("updater-" + n).start(() -> updater("updater-" + n, mainEnd)));
        }
        for (int i = 0; i < 2; i++) {
            int n = i;
            clients.add(Thread.ofVirtual().name("querier-" + n).start(() -> querier("querier-" + n, mainEnd)));
        }
        List<Thread> nemesisThreads = new ArrayList<>();
        for (int i = 0; i < nemeses; i++) {
            nemesisThreads.add(Thread.ofVirtual().name("nemesis-" + i).start(() -> nemesis(mainEnd)));
        }

        for (Thread t : nemesisThreads) {
            t.join();
        }
        Processes.sleepMillis(mainEnd - System.currentTimeMillis());
        mainPhase.set(false);

        log("recovery: healing");
        heal();
        history.nemesis("stop", "heal", "all");
        for (Thread t : clients) {
            t.join(Duration.ofMinutes(4)); // an update waits up to 3 minutes for its reply
        }
        retryPendingStarts(System.currentTimeMillis() + 120_000);
        log("recovery: closing " + ids.size() + " executions");
        closeAll(System.currentTimeMillis() + 120_000);
        // keep signalling executions while they finish: a signal acknowledged before an execution
        // completes must still reach its result (the server defers a completion while messages
        // are pending)
        AtomicBoolean finishing = new AtomicBoolean(true);
        Thread late = Thread.ofVirtual().name("late-signaler").start(() -> {
            while (finishing.get()) {
                Processes.sleepMillis(150);
                List<String> open = ids.keySet().stream().filter(k -> !finished.contains(k)).toList();
                if (!open.isEmpty()) {
                    deposit(dice.pick(open), "late-signaler");
                }
            }
        });
        Map<String, Long> converged = awaitTerminal(System.currentTimeMillis() + recoverySeconds * 1000L);
        finishing.set(false);
        late.join();
        // a query of every execution after it ended must return exactly its final deposits
        for (String key : ids.keySet()) {
            query(key, "final-querier");
        }

        log("final: reading executions");
        readFinal(converged);
        workers.stopAll();
        proxy.close();
        if (clientProxy != null) {
            clientProxy.close();
        }
        for (Proxy p : dbProxies) {
            p.close();
        }
        history.close();

        Checker.Report report = Checker.check(out);
        Files.writeString(out.resolve("report.json"), JSON.writeValueAsString(report));
        System.out.println();
        System.out.println(report.render());
        System.out.println("results in " + out.toAbsolutePath());
        return report.valid() ? 0 : 1;
    }

    // ---- clients ----

    private void starter(long mainEnd) {
        long window = Math.max(1, (mainEnd - System.currentTimeMillis()) * 6 / 10);
        long begin = System.currentTimeMillis();
        for (int i = 0; i < workflows; i++) {
            Processes.sleepMillis(begin + window * i / workflows - System.currentTimeMillis());
            String key = runId + "-" + i;
            if (!start(key, "starter")) {
                pendingStarts.add(key);
            }
            if (i % 5 == 4) {
                retryPendingStarts(0);
            }
        }
        while (mainPhase.get()) {
            retryPendingStarts(0);
            Processes.sleepMillis(2000);
        }
    }

    /** Starts (or finds again, by idempotency key) the execution for {@code key}. */
    private boolean start(String key, String process) {
        LedgerInput input = new LedgerInput(key, 1 + Math.floorMod(key.hashCode(), 4));
        history.invoke(process, "start", key, null);
        try {
            WorkflowHandle<LedgerResult> handle = conductor.workflow(LedgerWorkflow::run)
                    .idempotencyKey(key).correlationId(key).start(input);
            history.ok(process, "start", key, handle.workflowId());
            if (handle.workflowId() == null || handle.workflowId().isBlank()) {
                return false;
            }
            String previous = ids.putIfAbsent(key, handle.workflowId());
            if (previous != null && !previous.equals(handle.workflowId())) {
                log("ANOMALY idempotent start of " + key + " returned " + handle.workflowId() + " after " + previous);
            }
            if (previous == null && signalOnStart) {
                // aim at the window where the first continuation runs, before ownership is settled
                deposit(key, process);
            }
            return true;
        } catch (Exception e) {
            history.failed(process, "start", key, e);
            return false;
        }
    }

    private void retryPendingStarts(long until) {
        do {
            for (String key : List.copyOf(pendingStarts)) {
                if (start(key, "starter")) {
                    pendingStarts.remove(key);
                }
            }
            if (pendingStarts.isEmpty() || System.currentTimeMillis() > until) {
                return;
            }
            Processes.sleepMillis(2000);
        } while (true);
    }

    private void signaler(long mainEnd) {
        while (System.currentTimeMillis() < mainEnd) {
            Processes.sleepMillis(signalMs);
            String key = randomStarted();
            if (key == null) {
                continue;
            }
            deposit(key, "signaler");
        }
    }

    private void deposit(String key, String process) {
        String depositId = "dep-" + sequence.incrementAndGet();
        history.invoke(process, "deposit", key, depositId);
        try {
            conductor.attach(LedgerWorkflow::run, ids.get(key)).signal(LedgerWorkflow::deposit, depositId);
            history.ok(process, "deposit", key, depositId);
        } catch (Exception e) {
            history.failed(process, "deposit", key, e);
        }
    }

    private void updater(String process, long mainEnd) {
        while (System.currentTimeMillis() < mainEnd) {
            Processes.sleepMillis(updateMs);
            String key = randomStarted();
            if (key == null) {
                continue;
            }
            String updateId = "upd-" + sequence.incrementAndGet();
            history.invoke(process, "bump", key, updateId);
            try {
                int value = conductor.attach(LedgerWorkflow::run, ids.get(key)).update(LedgerWorkflow::bump, updateId);
                history.ok(process, "bump", key, Processes.mapOf("id", updateId, "value", value));
            } catch (Exception e) {
                history.failed(process, "bump", key, e);
            }
        }
    }

    private void querier(String process, long mainEnd) {
        while (System.currentTimeMillis() < mainEnd) {
            Processes.sleepMillis(400);
            String key = randomStarted();
            if (key != null) {
                query(key, process);
            }
        }
    }

    private void query(String key, String process) {
        history.invoke(process, "query", key, null);
        try {
            List<String> answer = conductor.attach(LedgerWorkflow::run, ids.get(key)).query(LedgerWorkflow::depositsSoFar);
            history.ok(process, "query", key, answer);
        } catch (Exception e) {
            history.failed(process, "query", key, e);
        }
    }

    private String randomStarted() {
        List<String> keys = new ArrayList<>(ids.keySet());
        return keys.isEmpty() ? null : dice.pick(keys);
    }

    // ---- nemesis ----

    private void nemesis(long mainEnd) {
        nemesis.run(mainEnd);
    }

    private void heal() throws Exception {
        nemesis.heal();
    }

    // ---- recovery and final ----

    private void closeAll(long until) {
        Set<String> open = ConcurrentHashMap.newKeySet();
        open.addAll(ids.keySet());
        while (!open.isEmpty() && System.currentTimeMillis() < until) {
            for (String key : List.copyOf(open)) {
                String id = ids.get(key);
                String status = status(id);
                if (status != null && !status.equals("RUNNING") && !status.equals("PAUSED")) {
                    open.remove(key);
                    continue;
                }
                history.invoke("closer", "close", key, null);
                try {
                    conductor.attach(LedgerWorkflow::run, id).signal(LedgerWorkflow::close);
                    history.ok("closer", "close", key, null);
                    open.remove(key);
                } catch (Exception e) {
                    history.failed("closer", "close", key, e);
                }
            }
            if (!open.isEmpty()) {
                Processes.sleepMillis(2000);
            }
        }
    }

    private Map<String, Long> awaitTerminal(long until) {
        Map<String, Long> doneAt = new LinkedHashMap<>();
        long lastLog = 0;
        while (System.currentTimeMillis() < until) {
            for (Map.Entry<String, String> e : ids.entrySet()) {
                if (!doneAt.containsKey(e.getKey())) {
                    String status = status(e.getValue());
                    if (status != null && !status.equals("RUNNING") && !status.equals("PAUSED")) {
                        doneAt.put(e.getKey(), System.currentTimeMillis());
                        finished.add(e.getKey());
                    }
                }
            }
            if (doneAt.size() == ids.size()) {
                break;
            }
            if (System.currentTimeMillis() - lastLog > 15_000) {
                lastLog = System.currentTimeMillis();
                log("recovery: " + doneAt.size() + "/" + ids.size() + " executions terminal");
            }
            Processes.sleepMillis(2000);
        }
        return doneAt;
    }

    @SuppressWarnings("unchecked")
    private String status(String workflowId) {
        try {
            String body = api.get("/workflow/" + workflowId + "/status");
            return body == null ? null : (String) JSON.readValue(body, Map.class).get("status");
        } catch (Exception e) {
            return null;
        }
    }

    private void readFinal(Map<String, Long> doneAt) throws Exception {
        Map<String, Object> byKey = new LinkedHashMap<>();
        for (int i = 0; i < workflows; i++) {
            String key = runId + "-" + i;
            String body = api.get("/workflow/durability_ledger/correlated/" + key + "?includeClosed=true&includeTasks=true");
            List<Map<String, Object>> executions =
                    body == null ? List.of() : JSON.readValue(body, new TypeReference<>() {});
            byKey.put(key, executions.stream().map(Bench::summarize).toList());
        }
        Map<String, Object> byId = new LinkedHashMap<>();
        for (String id : ids.values()) {
            String body = api.get("/workflow/" + id + "?includeTasks=true");
            byId.put(id, body == null ? null : summarize(JSON.readValue(body, new TypeReference<>() {})));
        }
        Files.writeString(out.resolve("final.json"), JSON.writeValueAsString(
                Processes.mapOf("byKey", byKey, "byId", byId, "doneAt", doneAt)));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> summarize(Map<String, Object> wf) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Map<String, Object> t : (List<Map<String, Object>>) wf.getOrDefault("tasks", List.of())) {
            tasks.add(Processes.mapOf(
                    "seq", t.get("seq"), "ref", t.get("referenceTaskName"), "type", t.get("taskType"),
                    "status", t.get("status"), "retryCount", t.get("retryCount"), "workerId", t.get("workerId"),
                    "domain", t.get("domain"), "reason", t.get("reasonForIncompletion"),
                    "scheduledTime", t.get("scheduledTime"), "startTime", t.get("startTime"),
                    "endTime", t.get("endTime"),
                    "continuation", t.get("continuation"),
                    "output", "durability_step".equals(t.get("taskType")) ? t.get("outputData") : null));
        }
        Map<String, Object> variables = (Map<String, Object>) wf.getOrDefault("variables", Map.of());
        return Processes.mapOf(
                "workflowId", wf.get("workflowId"), "correlationId", wf.get("correlationId"),
                "status", wf.get("status"), "reason", wf.get("reasonForIncompletion"),
                "createTime", wf.get("createTime"), "endTime", wf.get("endTime"),
                "output", wf.get("output"),
                "nativeState", wf.get("nativeState"),
                "variables", Processes.mapOf("deposits", variables.get("deposits"), "bumps", variables.get("bumps"),
                        "closed", variables.get("closed")),
                "tasks", tasks);
    }

    /**
     * Waits until every worker has registered its definitions. Executions take a snapshot of the
     * task definitions when they start, so an execution started before registration keeps
     * whatever an earlier run registered, such as dev mode's hour-long continuation timeout.
     */
    private void waitForDefinition() {
        long deadline = System.currentTimeMillis() + 120_000;
        for (Slot slot : workers.slots()) {
            Path log = out.resolve("worker-" + slot.workerId + ".log");
            while (true) {
                try {
                    if (Files.exists(log) && Files.readString(log).contains("started, pid")) {
                        break;
                    }
                } catch (java.io.IOException ignored) {
                    // not written yet
                }
                if (System.currentTimeMillis() > deadline) {
                    throw new IllegalStateException("worker " + slot.workerId + " did not start");
                }
                Processes.sleepMillis(500);
            }
        }
        while (api.get("/metadata/workflow/durability_ledger") == null) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("workers did not register durability_ledger");
            }
            Processes.sleepMillis(1000);
        }
    }

    static void log(String message) {
        System.out.println("[bench " + java.time.LocalTime.now().withNano(0) + "] " + message);
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v;
    }

    private static int intEnv(String name, int fallback) {
        return Integer.parseInt(env(name, Integer.toString(fallback)));
    }
}
