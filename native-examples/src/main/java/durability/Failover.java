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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.conductoross.conductor.sdk.testing.DurableReplay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import durability.FailoverTargets.Fault;
import durability.FailoverTargets.Handover;
import durability.FailoverTargets.Outcome;
import durability.Processes.Workers.Slot;
import kitchensink.KitchenSinkWorkflow;
import kitchensink.SubFlow;

/**
 * Worker failover, end to end, under Jepsen-style faults on the workers: the kitchensink runs on
 * several worker processes, and the bench aims faults at the worker that holds an execution, then
 * proves from the server's records that the execution did not wait for that worker and finished.
 * The server is never touched.
 *
 * <p>The faults, each on the worker that holds an execution (the one running its slow_step, or the
 * one it is pinned to, which keeps its code in memory):
 *
 * <ul>
 *   <li>{@code kill}: SIGKILL. The worker comes back {@code FAILOVER_RESTART_SECONDS} later as a new
 *       host. Aimed first at each point of the code in {@link FailoverTargets#TARGETS} (a slow_step,
 *       a Java loop, a doWhile, a loop the server runs, a switch case, a fork lane, a timer, a human
 *       task, an external wait, a child workflow, the code running), then at random.
 *   <li>{@code pause}: SIGSTOP for longer than every lease, then SIGCONT: the worker wakes up as a
 *       zombie that still thinks it holds its executions and its leases.
 *   <li>{@code gc-pause}: a burst of short SIGSTOPs, as a JVM in stop-the-world collections: every
 *       thread stops while the clocks run on. A signal gives the lengths and times a real collector
 *       will not.
 *   <li>{@code partition}: the worker cut off from the server while it keeps running. Each worker
 *       reaches the server through its own {@link Proxy}.
 * </ul>
 *
 * Kills run on one thread and the other faults on another. By default one fault is on at a time,
 * started only while every worker is healthy (up for 15 s, with no fault on it), so that each
 * fault's outcome is its own; with {@code FAILOVER_OVERLAP=true} they overlap, Jepsen style, as
 * long as one worker stays healthy. Several drivers ({@code kitchensink.Main} hosting no
 * workers) run their three executions at once, waiting {@code FAILOVER_ANSWER_DELAY_MS} before
 * answering a human task or a callback. Exit status 0 when every fault was handed over or ridden
 * out ({@link FailoverTargets#handover}), each fault type and kill target was hit, every execution
 * and child completed and replays against its code, every driver check passed and no worker logged
 * a divergence.
 *
 * <pre>
 *   FAILOVER_SERVER_URL       the server (default http://localhost:8099/api; never port 8080). It
 *                             needs conductor.workflow-message-queue.enabled=true and
 *                             conductor.ai.litellm.baseURL=http://localhost:8765, and the stub
 *                             stubs/ai_stub.py on 8765, as the kitchensink does
 *   FAILOVER_FAULTS           default kill,pause,gc-pause,partition
 *   FAILOVER_WORKERS          worker processes (default 3)
 *   FAILOVER_DRIVERS          kitchensink drivers at once, three executions each (default 4); while
 *                             a kill target or a quota is still open, one that ends is followed
 *                             by another
 *   FAILOVER_KILL_INTERVAL    seconds between random kills, once every target was hit (default 15)
 *   FAILOVER_FAULT_GAP        seconds between the other faults (default 15)
 *   FAILOVER_RESTART_SECONDS  when a killed worker comes back, as a new host (default 20; -1 never)
 *   FAILOVER_PAUSE_SECONDS    how long a pause or a partition lasts (default 75-105)
 *   FAILOVER_PAUSES, FAILOVER_PARTITIONS, FAILOVER_GC_PAUSES
 *                             how many must be handed over, or ridden out (default 2, 2, 4)
 *   FAILOVER_REQUIRE          kill targets that must be hit (default all but continuation, which
 *                             lasts milliseconds and is hit when a scan happens to see it)
 *   FAILOVER_SLOW_MS          how long a slow_step runs (default 20000)
 *   FAILOVER_ANSWER_DELAY_MS  how long a person or a system takes to answer (default 20000)
 *   FAILOVER_OVERLAP          true: faults overlap, as long as one worker stays healthy; a
 *                             continuation that waited while other workers had faults too is
 *                             then OVERLAPPED, not judged. Default false: one fault at a time
 *   FAILOVER_PROXY_PORT       the first worker's proxy; the others follow (default 18300)
 *   FAILOVER_TIMEOUT_MINUTES  for the drivers, then again for the executions to end (default 40)
 *   FAILOVER_SEED             which executions and durations are picked (default: the time)
 *   FAILOVER_OUT              output directory (default build/failover/&lt;time&gt;)
 * </pre>
 */
public class Failover {

    private static final Pattern SLOW_STEP = Pattern.compile("^slow_step (\\S+) running on (\\S+)", Pattern.MULTILINE);
    private static final Set<String> TERMINAL = Set.of("COMPLETED", "FAILED", "TERMINATED", "TIMED_OUT");
    /** What a pause or a partition is aimed at: something of the execution comes due soon. */
    private static final Set<String> DUE_SOON = Set.of("java-loop", "doWhile", "server-loop", "switch", "fork",
            "timer", "child", "continuation");

    private final String url;
    private final Path out;
    private final Processes.Api api;
    private final Processes.Workers workers;
    private final List<Proxy> proxies = new ArrayList<>();
    private final Set<String> faults;
    private final int driverCount;
    private final int killInterval;
    private final int faultGap;
    private final int restartSeconds;
    private final int[] pauseSeconds;
    private final Map<String, Integer> quotas;
    private final long answerDelayMs;
    private final int targetWait = 90;
    private final boolean overlap;
    /** How long a started worker takes to register and poll before it counts as healthy. */
    private static final long BOOT_MS = 15_000;
    /** The claim in {@link #busy} that, without overlap, lets one fault be chosen and run at a time. */
    private static final int ANY = -1;
    private final Map<Integer, Long> startedAt = new ConcurrentHashMap<>();
    /** Time a driver round needs: another starts only while this much of the timeout is left. */
    private static final long ROUND_MS = 12 * 60_000;
    private final Set<String> required;
    private final Duration timeout;
    private final Random random;

    /** Every worker id ever started, to its slot; the ids of killed workers never come back. */
    private final Map<String, Slot> byId = new ConcurrentHashMap<>();
    private final Set<String> dead = ConcurrentHashMap.newKeySet();
    /** The slots a fault is on, by index: one fault at a time per worker. */
    private final Map<Integer, String> busy = new ConcurrentHashMap<>();
    private final List<Process> drivers = new CopyOnWriteArrayList<>();
    /** The kitchensink executions the drivers started, from their {@code started <id>} lines. */
    private final Set<String> executions = new CopyOnWriteArraySet<>();
    private final Map<String, String> statuses = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> pins = new ConcurrentHashMap<>();
    private final List<Fault> injected = new CopyOnWriteArrayList<>();
    private volatile boolean driving = true;

    Failover(String url, Path out, Set<String> faults, int workerCount, int driverCount, int killInterval,
            int faultGap, int restartSeconds, int[] pauseSeconds, Map<String, Integer> quotas, long slowMs,
            long answerDelayMs, int proxyPort, Set<String> required, Duration timeout, long seed) throws IOException {
        this.url = url;
        this.out = out;
        this.api = new Processes.Api(url);
        this.faults = faults;
        int serverPort = URI.create(url).getPort();
        for (int i = 0; i < workerCount; i++) {
            proxies.add(new Proxy(proxyPort + i, URI.create(url).getHost(), serverPort));
        }
        this.workers = new Processes.Workers(workerCount, slot -> "http://127.0.0.1:" + (proxyPort + slot) + "/api", out,
                Map.of("KITCHENSINK_SLOW_MS", Long.toString(slowMs), "KITCHENSINK_STATE_DIR", out.resolve("state").toString()),
                "kitchensink.Worker");
        this.driverCount = driverCount;
        this.killInterval = killInterval;
        this.faultGap = faultGap;
        this.restartSeconds = restartSeconds;
        this.pauseSeconds = pauseSeconds;
        this.quotas = quotas;
        this.answerDelayMs = answerDelayMs;
        this.overlap = Boolean.parseBoolean(env("FAILOVER_OVERLAP", "false"));
        this.required = required;
        this.timeout = timeout;
        this.random = new Random(seed);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("check")) {
            System.exit(check(Path.of(args[1])) ? 0 : 1);
        }
        String url = env("FAILOVER_SERVER_URL", "http://localhost:8099/api");
        if (URI.create(url).getPort() == 8080) {
            throw new IllegalArgumentException("refusing to use port 8080");
        }
        Set<String> faults = csv(env("FAILOVER_FAULTS", "kill,pause,gc-pause,partition"));
        Set<String> required = csv(env("FAILOVER_REQUIRE",
                String.join(",", FailoverTargets.TARGETS.stream().filter(t -> !t.equals("continuation")).toList())));
        int[] pause = Arrays.stream(env("FAILOVER_PAUSE_SECONDS", "75-105").split("-")).mapToInt(Integer::parseInt).toArray();
        Map<String, Integer> quotas = Map.of("pause", intEnv("FAILOVER_PAUSES", 2),
                "partition", intEnv("FAILOVER_PARTITIONS", 2), "gc-pause", intEnv("FAILOVER_GC_PAUSES", 4));
        long seed = Long.parseLong(env("FAILOVER_SEED", Long.toString(System.currentTimeMillis())));
        Path out = Path.of(env("FAILOVER_OUT", "build/failover/"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))));
        Files.createDirectories(out.resolve("executions"));
        Files.createDirectories(out.resolve("state"));
        Map<String, Object> config = Processes.mapOf("url", url, "faults", faults, "workers", intEnv("FAILOVER_WORKERS", 3),
                "drivers", intEnv("FAILOVER_DRIVERS", 4), "killInterval", intEnv("FAILOVER_KILL_INTERVAL", 15),
                "faultGap", intEnv("FAILOVER_FAULT_GAP", 15), "restartSeconds", intEnv("FAILOVER_RESTART_SECONDS", 20),
                "pauseSeconds", pause[0] + "-" + pause[pause.length - 1], "quotas", quotas,
                "slowMs", intEnv("FAILOVER_SLOW_MS", 20_000), "answerDelayMs", intEnv("FAILOVER_ANSWER_DELAY_MS", 20_000),
                "required", required, "overlap", Boolean.parseBoolean(env("FAILOVER_OVERLAP", "false")), "seed", seed);
        Files.writeString(out.resolve("config.json"), Bench.JSON.writeValueAsString(config));
        Bench.log("failover run -> " + out.toAbsolutePath());
        Bench.log("config " + config);
        Failover bench = new Failover(url, out, faults, intEnv("FAILOVER_WORKERS", 3), intEnv("FAILOVER_DRIVERS", 4),
                intEnv("FAILOVER_KILL_INTERVAL", 15), intEnv("FAILOVER_FAULT_GAP", 15),
                intEnv("FAILOVER_RESTART_SECONDS", 20), pause, quotas, intEnv("FAILOVER_SLOW_MS", 20_000),
                intEnv("FAILOVER_ANSWER_DELAY_MS", 20_000), intEnv("FAILOVER_PROXY_PORT", 18300), required,
                Duration.ofMinutes(intEnv("FAILOVER_TIMEOUT_MINUTES", 40)), seed);
        System.exit(bench.run() ? 0 : 1);
    }

    boolean run() throws Exception {
        // the workers and drivers are this run's: none outlives it, however it ends
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            drivers.forEach(Process::destroyForcibly);
            workers.slots().forEach(s -> {
                if (s.process != null) {
                    Processes.signal(s.process.pid(), "CONT");
                    s.process.destroyForcibly();
                }
            });
        }));
        long started = System.currentTimeMillis();
        workers.startAll();
        workers.slots().forEach(s -> {
            byId.put(s.workerId, s);
            startedAt.put(s.index, System.currentTimeMillis());
        });
        awaitRegistered();
        List<Process> running = new ArrayList<>();
        for (int i = 0; i < driverCount; i++) {
            running.add(startDriver());
            Processes.sleepMillis(2_000); // distinct idempotency keys, and the drivers at different points
        }
        Bench.log(driverCount + " drivers started on " + workers.slots().size() + " workers");
        Thread tracker = Thread.ofVirtual().name("tracker").start(this::track);
        Thread killer = Thread.ofVirtual().name("killer").start(this::kill);
        Thread stopper = Thread.ofVirtual().name("stopper").start(this::stop);

        // a driver that ends while a target or a quota is still open is followed by another round,
        // so that faults one at a time still have executions to land on
        long deadline = started + timeout.toMillis();
        while (!running.isEmpty() && System.currentTimeMillis() < deadline) {
            for (Process d : List.copyOf(running)) {
                if (!d.isAlive()) {
                    running.remove(d);
                    if (!covered() && System.currentTimeMillis() < deadline - ROUND_MS) {
                        running.add(startDriver());
                    }
                }
            }
            Processes.sleepMillis(1_000);
        }
        for (Process d : running) {
            Bench.log("a driver did not end in time");
            d.destroyForcibly().waitFor();
        }
        driving = false;
        killer.join(); // a worker killed last comes back on its own, as the others did
        stopper.join(); // and the last paused or cut off one is resumed or reconnected
        tracker.interrupt();
        tracker.join();
        return verify(System.currentTimeMillis() - started, System.currentTimeMillis() + timeout.toMillis());
    }

    private Process startDriver() throws IOException {
        int n = drivers.size();
        ProcessBuilder pb = KitchenSinkChaos.kitchensinkDriver(out.resolve("driver-" + n + ".log"), url);
        pb.environment().remove("CONDUCTOR_DEV");
        pb.environment().put("KITCHENSINK_ANSWER_DELAY_MS", Long.toString(answerDelayMs));
        Process driver = pb.start();
        drivers.add(driver);
        if (n >= driverCount) {
            Bench.log("driver " + n + " started: a target or a quota is still open");
        }
        return driver;
    }

    /** Whether every required kill target was hit and every quota reached, as far as can be told before the end. */
    private boolean covered() {
        Set<String> hit = new LinkedHashSet<>();
        injected.stream().filter(f -> f.type().equals("kill")).forEach(f -> hit.add(f.target()));
        if (faults.contains("kill") && !hit.containsAll(required)) {
            return false;
        }
        // one more pause or partition than needed: one may have had nothing due
        return quotas.entrySet().stream().allMatch(q -> !faults.contains(q.getKey())
                || injected.stream().filter(f -> f.type().equals(q.getKey())).count()
                        >= q.getValue() + (q.getKey().equals("gc-pause") ? 0 : 1));
    }

    /**
     * Waits until the workers registered the definitions they run with. An execution keeps the
     * definitions it started with, so one started over a dev-mode registration would keep its
     * hour-long continuation lease, and a fault mid-continuation would wait out the hour.
     */
    private void awaitRegistered() {
        long deadline = System.currentTimeMillis() + 180_000;
        while (!(lease("kitchensink") == 30 && lease("sub_flow_1") == 30
                && Integer.valueOf(60).equals(json(api.get("/metadata/taskdefs/slow_step")).get("responseTimeoutSeconds")))) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("the workers did not register the kitchensink with a 30 s continuation"
                        + " lease and slow_step with a 60 s response timeout");
            }
            Processes.sleepMillis(1_000);
        }
    }

    private long lease(String workflow) {
        return FailoverTargets.num(FailoverTargets.map(FailoverTargets.map(
                json(api.get("/metadata/workflow/" + workflow)).get("metadata")).get("native")).get("continuationTimeoutSeconds"));
    }

    // ---------------------------------------------------------------- tracking

    /** Every 500 ms, each execution's status and the worker it is pinned to, kept when they change. */
    private void track() {
        Map<String, String> last = new ConcurrentHashMap<>();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                readStartedExecutions();
                for (String id : List.copyOf(executions)) {
                    if (TERMINAL.contains(statuses.getOrDefault(id, ""))) {
                        continue;
                    }
                    Map<String, Object> wf = json(api.get("/workflow/" + id + "?includeTasks=false"));
                    String status = String.valueOf(wf.get("status"));
                    Object pinnedTo = FailoverTargets.map(wf.get("nativeState")).get("pinnedTo");
                    statuses.put(id, status);
                    String now = status + " " + pinnedTo;
                    if (!now.equals(last.put(id, now))) {
                        Map<String, Object> sample = Processes.mapOf("t", System.currentTimeMillis(), "workflowId", id,
                                "status", status, "pinnedTo", pinnedTo);
                        pins.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(sample);
                        append("holders.jsonl", sample);
                    }
                }
            } catch (RuntimeException e) {
                Bench.log("tracker: " + e); // the next round reads again
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void readStartedExecutions() {
        for (int i = 0; i < drivers.size(); i++) {
            Matcher m = KitchenSinkChaos.STARTED.matcher(read(out.resolve("driver-" + i + ".log")));
            while (m.find()) {
                executions.add(m.group(1));
            }
        }
    }

    // ---------------------------------------------------------------- the faults

    /** Kills: at every kill target not hit yet, whichever an execution reaches first; then at random. */
    private void kill() {
        if (!faults.contains("kill")) {
            return;
        }
        long lastKill = System.currentTimeMillis();
        while (driving) {
            try {
                Set<String> open = new LinkedHashSet<>(FailoverTargets.TARGETS);
                injected.stream().filter(f -> f.type().equals("kill")).forEach(f -> open.remove(f.target()));
                boolean randomDue = System.currentTimeMillis() - lastKill > killInterval * 1000L
                        && open.stream().noneMatch(required::contains);
                if ((!open.isEmpty() && aim("kill", open)) || (randomDue && aim("kill", Set.of("random")))) {
                    lastKill = System.currentTimeMillis();
                }
            } catch (RuntimeException e) {
                Bench.log("killer: " + e);
            }
            Processes.sleepMillis(300);
        }
    }

    /** Pauses, GC pauses and partitions, in turn, each aimed for up to 90 s, a gap between them. */
    private void stop() {
        List<String> kinds = List.of("pause", "gc-pause", "partition").stream().filter(faults::contains).toList();
        for (int turn = 0; driving && !kinds.isEmpty(); turn++) {
            String kind = kinds.get(turn % kinds.size());
            long until = System.currentTimeMillis() + targetWait * 1000L;
            try {
                // something of the execution comes due soon, or any running one when nothing does
                while (driving && !aim(kind, kind.equals("gc-pause") ? Set.of("random") : DUE_SOON)) {
                    if (System.currentTimeMillis() > until) {
                        aim(kind, Set.of("random"));
                        break;
                    }
                    Processes.sleepMillis(300);
                }
            } catch (RuntimeException e) {
                Bench.log("stopper: " + e);
            }
            Processes.sleepMillis(faultGap * 1000L);
        }
    }

    /**
     * One scan of the running executions: injects {@code kind} on the holder of the first that is
     * at one of {@code targets} ("random": any), and returns whether it did. Without {@code
     * overlap}, a fault starts only while every worker is healthy, so that each fault's outcome is
     * its own; with it, while a fault would leave at least one worker healthy.
     */
    private boolean aim(String kind, Set<String> targets) {
        if (!overlap && busy.putIfAbsent(ANY, kind) != null) {
            return false; // the other thread is choosing, or its fault is on
        }
        try {
            long healthy = workers.slots().stream().filter(this::healthy).count();
            return (overlap ? healthy >= 2 : healthy == workers.slots().size()) && inject(kind, targets);
        } finally {
            if (!overlap) {
                busy.remove(ANY);
            }
        }
    }

    /** Up, with no fault on it, and up long enough to have registered and polled. */
    private boolean healthy(Slot slot) {
        return slot.alive() && !busy.containsKey(slot.index)
                && System.currentTimeMillis() - startedAt.getOrDefault(slot.index, 0L) >= BOOT_MS;
    }

    private boolean inject(String kind, Set<String> targets) {
        List<String> running = new ArrayList<>(executions.stream().filter(id -> "RUNNING".equals(statuses.get(id))).toList());
        Collections.shuffle(running, random);
        Map<String, String> runners = targets.contains("long-task") ? slowStepRunners() : Map.of();
        for (String id : running) {
            Map<String, Object> wf = json(api.get("/workflow/" + id + "?includeTasks=true"));
            Set<String> features = FailoverTargets.features(wf);
            String target = targets.contains("random") ? "random"
                    : targets.stream().filter(features::contains).findFirst().orElse(null);
            if (target == null) {
                continue;
            }
            String holder = FailoverTargets.holder(wf, target, runners);
            Slot slot = holder == null || dead.contains(holder) ? null : byId.get(holder);
            if (slot == null || !healthy(slot) || busy.putIfAbsent(slot.index, kind) != null) {
                continue;
            }
            String slowStep = "long-task".equals(target) ? FailoverTargets.runningSlowStep(wf) : null;
            switch (kind) {
                case "kill" -> killOne(slot, id, target, slowStep, features);
                case "pause" -> pauseOne(slot, id, target, features);
                case "gc-pause" -> gcPauses(slot, id, target, features);
                default -> partitionOne(slot, id, target, features);
            }
            return true;
        }
        return false;
    }

    private void killOne(Slot slot, String workflowId, String target, String slowStep, Set<String> features) {
        String worker = slot.workerId;
        dead.add(worker);
        try {
            workers.kill(slot); // SIGKILL, then waits for the exit
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (slot.alive()) {
            throw new IllegalStateException("worker " + worker + " did not die");
        }
        record(new Fault("kill", System.currentTimeMillis(), Long.MAX_VALUE, workflowId, target, worker, slowStep, features));
        if (restartSeconds < 0) {
            return; // the slot stays busy: never restarted
        }
        Thread.ofVirtual().start(() -> {
            Processes.sleepMillis(restartSeconds * 1000L);
            String id = workers.freshId(slot); // a new host takes the dead one's place
            try {
                workers.start(slot, id);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            byId.put(id, slot);
            startedAt.put(slot.index, System.currentTimeMillis());
            busy.remove(slot.index);
            Bench.log("worker " + id + " started in place of " + worker);
        });
    }

    private void pauseOne(Slot slot, String workflowId, String target, Set<String> features) {
        long seconds = between(pauseSeconds[0], pauseSeconds[pauseSeconds.length - 1]);
        long at = System.currentTimeMillis();
        workers.pause(slot); // SIGSTOP
        Bench.log("paused " + slot.workerId + " holding " + workflowId + " at " + target + " " + features + " for " + seconds + " s");
        Processes.sleepMillis(seconds * 1000L);
        workers.resume(slot); // SIGCONT: it wakes up as a zombie
        record(new Fault("pause", at, System.currentTimeMillis(), workflowId, target, slot.workerId, null, features));
        busy.remove(slot.index);
    }

    private void gcPauses(Slot slot, String workflowId, String target, Set<String> features) {
        long at = System.currentTimeMillis();
        int pauses = (int) between(2, 5);
        StringBuilder lengths = new StringBuilder();
        for (int i = 0; i < pauses; i++) {
            long ms = between(500, 8_000);
            lengths.append(i == 0 ? "" : ", ").append(ms).append(" ms");
            workers.pause(slot);
            Processes.sleepMillis(ms);
            workers.resume(slot);
            Processes.sleepMillis(between(200, 2_000));
        }
        Bench.log("gc-paused " + slot.workerId + " holding " + workflowId + " " + features + ": " + lengths);
        record(new Fault("gc-pause", at, System.currentTimeMillis(), workflowId, target, slot.workerId, null, features));
        busy.remove(slot.index);
    }

    private void partitionOne(Slot slot, String workflowId, String target, Set<String> features) {
        long seconds = between(pauseSeconds[0], pauseSeconds[pauseSeconds.length - 1]);
        Proxy proxy = proxies.get(slot.index);
        long at = System.currentTimeMillis();
        proxy.partition();
        Bench.log("cut off " + slot.workerId + " holding " + workflowId + " at " + target + " " + features + " for " + seconds + " s");
        Processes.sleepMillis(seconds * 1000L);
        proxy.heal();
        record(new Fault("partition", at, System.currentTimeMillis(), workflowId, target, slot.workerId, null, features));
        busy.remove(slot.index);
    }

    private void record(Fault f) {
        injected.add(f);
        append("faults.jsonl", Processes.mapOf("type", f.type(), "at", f.at(), "until", f.until() == Long.MAX_VALUE ? null : f.until(),
                "workflowId", f.workflowId(), "target", f.target(), "worker", f.worker(), "slowStep", f.slowStepTaskId(),
                "features", f.features()));
        if (f.type().equals("kill")) {
            Bench.log("killed " + f.worker() + " holding " + f.workflowId() + " at " + f.target() + " " + f.features());
        }
    }

    private long between(long lo, long hi) {
        return lo + (long) (random.nextDouble() * (hi - lo + 1));
    }

    private Map<String, String> slowStepRunners() {
        return slowStepRunners(out);
    }

    /** Which worker ran each slow_step, from the line it logs (tasks report the host name as their worker id). */
    private static Map<String, String> slowStepRunners(Path out) {
        Map<String, String> runners = new ConcurrentHashMap<>();
        try (Stream<Path> logs = Files.list(out)) {
            logs.filter(p -> p.getFileName().toString().startsWith("worker-")).forEach(p -> {
                Matcher m = SLOW_STEP.matcher(read(p));
                while (m.find()) {
                    runners.put(m.group(1), m.group(2));
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return runners;
    }

    // ---------------------------------------------------------------- the end

    /** The execution's record once it has ended, or as it is at the deadline. */
    private String ended(String id, long deadline) {
        while (true) {
            String body = api.get("/workflow/" + id + "?includeTasks=true");
            if (TERMINAL.contains(String.valueOf(json(body).get("status"))) || System.currentTimeMillis() > deadline) {
                return body;
            }
            Processes.sleepMillis(2_000);
        }
    }

    private boolean verify(long wallMs, long deadline) throws IOException {
        readStartedExecutions();
        List<String> pending = new ArrayList<>(executions);
        for (int i = 0; i < pending.size(); i++) {
            String id = pending.get(i);
            // children too: a workflow started by name may still run when its parent ends
            String body = ended(id, deadline);
            Files.writeString(out.resolve("executions").resolve(id + ".json"), body == null ? "{}" : body);
            for (String child : FailoverTargets.children(json(body))) {
                if (!pending.contains(child)) {
                    pending.add(child);
                }
            }
        }
        List<Integer> exits = drivers.stream().map(Process::exitValue).toList();
        return judge(out, injected, pins, faults, required, quotas, exits, wallMs);
    }

    /**
     * Re-judges a finished run from what it saved: {@code durability.Failover check <run dir>}.
     * The driver exit codes are not saved; a driver passed when its log says so.
     */
    static boolean check(Path out) throws IOException {
        Map<String, Object> config = json(read(out.resolve("config.json")));
        List<Fault> injected = new ArrayList<>();
        for (Map<String, Object> f : Checker.readLines(out.resolve("faults.jsonl"))) {
            long until = f.get("until") == null ? Long.MAX_VALUE : FailoverTargets.num(f.get("until"));
            injected.add(new Fault(String.valueOf(f.get("type")), FailoverTargets.num(f.get("at")), until,
                    String.valueOf(f.get("workflowId")), String.valueOf(f.get("target")), String.valueOf(f.get("worker")),
                    (String) f.get("slowStep"), new LinkedHashSet<>(FailoverTargets.list(f.get("features")).stream().map(String::valueOf).toList())));
        }
        Map<String, List<Map<String, Object>>> pins = new LinkedHashMap<>();
        for (Map<String, Object> p : Checker.readLines(out.resolve("holders.jsonl"))) {
            pins.computeIfAbsent(String.valueOf(p.get("workflowId")), k -> new ArrayList<>()).add(p);
        }
        Map<String, Integer> quotas = new LinkedHashMap<>();
        FailoverTargets.map(config.get("quotas")).forEach((k, v) -> quotas.put(k, (int) FailoverTargets.num(v)));
        List<Integer> exits = new ArrayList<>();
        for (int i = 0; Files.exists(out.resolve("driver-" + i + ".log")); i++) {
            exits.add(read(out.resolve("driver-" + i + ".log")).contains("KITCHENSINK PASSED") ? 0 : 1);
        }
        return judge(out, injected, pins, strings(config.get("faults")), strings(config.get("required")), quotas, exits, -1);
    }

    private static Set<String> strings(Object list) {
        return new LinkedHashSet<>(FailoverTargets.list(list).stream().map(String::valueOf).toList());
    }

    /** Judges a run from its saved records ({@code executions/}), logs and faults, and writes its report. */
    private static boolean judge(Path out, List<Fault> injected, Map<String, List<Map<String, Object>>> pins,
            Set<String> faults, Set<String> required, Map<String, Integer> quotas, List<Integer> exits, long wallMs)
            throws IOException {
        List<String> problems = new ArrayList<>();
        Map<String, Map<String, Object>> records = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(out.resolve("executions"))) {
            for (Path file : files.sorted().toList()) {
                Map<String, Object> wf = json(read(file));
                String id = String.valueOf(wf.get("workflowId"));
                records.put(id, wf);
                String name = String.valueOf(wf.get("workflowName"));
                if (!"COMPLETED".equals(wf.get("status"))) {
                    problems.add("execution " + id + " (" + name + ") is " + wf.get("status") + ": " + wf.get("reasonForIncompletion"));
                    continue;
                }
                try {
                    DurableReplay.assertCompatible("sub_flow_1".equals(name) ? SubFlow.class : KitchenSinkWorkflow.class, file);
                } catch (Throwable t) {
                    problems.add("execution " + id + " (" + name + ") does not replay against its code: " + t.getMessage());
                }
            }
        }

        int passed = 0;
        int failed = 0;
        for (int i = 0; i < exits.size(); i++) {
            String log = read(out.resolve("driver-" + i + ".log"));
            passed += (int) log.lines().filter(l -> l.startsWith("  PASS")).count();
            failed += (int) log.lines().filter(l -> l.startsWith("  FAIL")).count();
            if (exits.get(i) != 0) {
                problems.add("driver " + i + " exited " + exits.get(i) + ": "
                        + log.lines().filter(l -> l.startsWith("  FAIL") || l.startsWith("KITCHENSINK") || l.contains("Exception"))
                                .limit(5).toList());
            }
        }
        try (Stream<Path> logs = Files.list(out)) {
            logs.filter(p -> p.getFileName().toString().startsWith("worker-")).forEach(p -> read(p).lines()
                    .filter(l -> l.contains("NonDeterminismException") || l.contains("diverged"))
                    .limit(3).forEach(l -> problems.add(p.getFileName() + ": " + l)));
        }

        Map<String, String> runners = slowStepRunners(out);
        Map<String, int[]> coverage = new TreeMap<>(); // kill target -> {kills, handed over}
        Map<String, Map<Outcome, Integer>> outcomes = new TreeMap<>(); // fault type -> outcome -> count
        Map<String, List<Long>> delays = new TreeMap<>();
        List<Map<String, Object>> faultReport = new ArrayList<>();
        System.out.println();
        System.out.println("faults:");
        for (Fault f : injected) {
            Handover h = FailoverTargets.handover(f, records.getOrDefault(f.workflowId(), Map.of()),
                    pins.getOrDefault(f.workflowId(), List.of()), runners, injected);
            outcomes.computeIfAbsent(f.type(), x -> new TreeMap<>()).merge(h.outcome(), 1, Integer::sum);
            if (f.type().equals("kill")) {
                for (String target : f.target().equals("random") ? f.features() : Set.of(f.target())) {
                    int[] c = coverage.computeIfAbsent(target, x -> new int[2]);
                    c[0]++;
                    c[1] += h.ok() ? 1 : 0;
                }
            }
            if (h.delayMs() != null) {
                delays.computeIfAbsent(f.type(), x -> new ArrayList<>()).add(h.delayMs());
            }
            if (!h.ok()) {
                problems.add(f.type() + " of " + f.worker() + " holding " + f.workflowId() + " at " + f.target() + ": " + h.why());
            }
            System.out.printf("  %-11s %-9s %-12s %-10s -> %-14s %9s  %s  %s%n", h.outcome(), f.type(), f.target(),
                    f.worker(), h.continuedBy() == null ? "-" : h.continuedBy().startsWith("a live") ? "(unpinned)" : h.continuedBy(),
                    h.delayMs() == null ? "-" : h.delayMs() + " ms", f.workflowId(), h.why());
            faultReport.add(Processes.mapOf("type", f.type(), "target", f.target(), "workflowId", f.workflowId(),
                    "worker", f.worker(), "features", f.features(), "outcome", h.outcome(), "why", h.why(),
                    "continuedBy", h.continuedBy(), "delayMs", h.delayMs()));
        }
        if (faults.contains("kill")) {
            for (String target : required) {
                int[] c = coverage.get(target);
                if (c == null || c[1] == 0) {
                    problems.add("kill target " + target + " was not hit");
                }
            }
        }
        quotas.forEach((type, quota) -> {
            Outcome needed = type.equals("gc-pause") ? Outcome.RODE_OUT : Outcome.HANDED_OVER;
            int got = outcomes.getOrDefault(type, Map.of()).getOrDefault(needed, 0);
            if (faults.contains(type) && got < quota) {
                problems.add(type + ": " + got + " " + needed + ", fewer than the " + quota + " required");
            }
        });

        List<Long> durations = new ArrayList<>();
        List<Long> latencies = new ArrayList<>();
        records.values().forEach(wf -> {
            if (FailoverTargets.num(wf.get("endTime")) > 0 && "kitchensink".equals(wf.get("workflowName"))) {
                durations.add(FailoverTargets.num(wf.get("endTime")) - FailoverTargets.num(wf.get("createTime")));
            }
            FailoverTargets.applied(wf).forEach(m -> latencies.add(m[1] - m[0]));
        });
        long completed = records.values().stream().filter(wf -> "COMPLETED".equals(wf.get("status"))).count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("wall time s", wallMs < 0 ? "-" : wallMs / 1000);
        stats.put("executions completed", completed + " / " + records.size());
        stats.put("kitchensink checks", passed + " passed, " + failed + " failed");
        stats.put("faults", outcomes);
        delays.forEach((type, d) -> stats.put(type + " failover delay ms p50/p95/max", percentiles(d, 0.95)));
        stats.put("execution duration ms p50/p95/max", percentiles(durations, 0.95));
        stats.put("continuations", latencies.size());
        stats.put("continuation latency ms p50/p99/max", percentiles(latencies, 0.99));

        System.out.println();
        System.out.println("kill targets (handed over / kills):");
        for (String target : FailoverTargets.TARGETS) {
            int[] c = coverage.getOrDefault(target, new int[2]);
            System.out.printf("  %-13s %d / %d%s%n", target, c[1], c[0], required.contains(target) ? "" : "  (not required)");
        }
        System.out.println();
        stats.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        System.out.println();
        problems.forEach(p -> System.out.println("PROBLEM " + p));
        Files.writeString(out.resolve("report.json"), Bench.JSON.writeValueAsString(Processes.mapOf(
                "ok", problems.isEmpty(), "problems", problems, "stats", stats, "faults", faultReport,
                "killTargets", coverage.entrySet().stream().collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), Map.of("kills", e.getValue()[0], "handedOver", e.getValue()[1])), Map::putAll))));
        System.out.println(problems.isEmpty() ? "FAILOVER PASSED" : "FAILOVER FAILED: " + problems.size() + " problems");
        Bench.log("results in " + out.toAbsolutePath());
        return problems.isEmpty();
    }

    private static String percentiles(List<Long> values, double high) {
        if (values.isEmpty()) {
            return "-";
        }
        List<Long> sorted = values.stream().sorted().toList();
        return sorted.get(sorted.size() / 2) + " / "
                + sorted.get(Math.min(sorted.size() - 1, (int) Math.ceil(sorted.size() * high) - 1)) + " / "
                + sorted.get(sorted.size() - 1);
    }

    // ---------------------------------------------------------------- helpers

    private static Map<String, Object> json(String body) {
        try {
            return body == null ? Map.of() : Bench.JSON.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void append(String file, Map<String, Object> line) {
        try {
            Files.writeString(out.resolve(file), Bench.JSON.writer().without(SerializationFeature.INDENT_OUTPUT)
                    .writeValueAsString(line) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.exists(file) ? Files.readString(file) : "";
        } catch (IOException e) {
            return "";
        }
    }

    private static Set<String> csv(String value) {
        return new LinkedHashSet<>(Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v;
    }

    private static int intEnv(String name, int fallback) {
        return Integer.parseInt(env(name, Integer.toString(fallback)));
    }
}
