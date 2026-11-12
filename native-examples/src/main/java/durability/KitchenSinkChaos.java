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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import durability.Processes.Dice;

/**
 * Runs the kitchensink while a nemesis injects faults, then lists the executions it ran so they
 * can be checked: by the kitchensink's own checks, and against the code with
 * {@code proposals/native-workflows/spec} ({@code check_paths.py}, {@code check_runs.py},
 * {@code kitchensink_spec.py --chaos}).
 *
 * <p>Worker processes ({@code kitchensink.Worker}) reach the server through a proxy that can be
 * partitioned; the driver ({@code kitchensink.Main} with {@code KITCHENSINK_HOST=false}) talks to
 * the server directly, as an application would. The faults are the bench's: workers killed with
 * SIGKILL and restarted (half the time as a new host), workers and the server paused with
 * SIGSTOP, the server killed and restarted, workers cut off from the server.
 *
 * <pre>
 *   CHAOS_SERVER_PORT     a server built from the conductor branch (default 8099; never 8080)
 *   CHAOS_SERVER_SCRIPT   runs that server in the foreground (exec java ...); needed for server-kill
 *   CHAOS_FAULTS          default worker-kill,worker-pause,partition; server-pause and server-kill
 *                         (with the script) also fail the driver's own calls, which are not
 *                         written to ride out a server outage as the bench's clients are
 *   CHAOS_FAULT_INTERVAL  mean seconds between faults (default 10)
 *   CHAOS_WORKERS         worker processes (default 2)
 *   CHAOS_ROUNDS          kitchensink runs, each of three executions (default 1)
 *   CHAOS_SEED            the nemesis's random seed (default: the time)
 *   CHAOS_TASK_ERRORS     per task call, the chance it throws an error the server retries
 *   CHAOS_TASK_FAILURES   per task call, the chance of a terminal failure (no retry): unless the
 *                         code catches it, the run fails at that task. With either set, a failed
 *                         execution is reported and the run goes on to the next one
 * </pre>
 * The MCP and A2A stub ({@code stubs/ai_stub.py}) must be running, as for the kitchensink.
 */
public class KitchenSinkChaos {

    private static final Pattern STARTED = Pattern.compile("^started (\\S+)", Pattern.MULTILINE);

    public static void main(String[] args) throws Exception {
        int serverPort = intEnv("CHAOS_SERVER_PORT", 8099);
        int proxyPort = intEnv("CHAOS_PROXY_PORT", 18197);
        if (serverPort == 8080 || proxyPort == 8080) {
            throw new IllegalArgumentException("refusing to use port 8080");
        }
        String script = System.getenv("CHAOS_SERVER_SCRIPT");
        List<String> faults = Arrays.stream(env("CHAOS_FAULTS", "worker-kill,worker-pause,partition").split(","))
                .map(String::trim).filter(f -> !f.isEmpty() && !f.equals("none")).toList();
        long seed = Long.parseLong(env("CHAOS_SEED", Long.toString(System.currentTimeMillis())));
        int workerCount = intEnv("CHAOS_WORKERS", 2);
        int rounds = intEnv("CHAOS_ROUNDS", 1);
        double interval = intEnv("CHAOS_FAULT_INTERVAL", 10);
        Path out = Path.of(env("CHAOS_OUT", "build/chaos/"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))));
        Files.createDirectories(out.resolve("ledger"));
        Map<String, Object> config = Processes.mapOf("serverPort", serverPort, "proxyPort", proxyPort,
                "faults", faults, "faultInterval", interval, "workers", workerCount, "rounds", rounds, "seed", seed,
                "taskErrors", env("CHAOS_TASK_ERRORS", "0"), "taskFailures", env("CHAOS_TASK_FAILURES", "0"));
        Files.writeString(out.resolve("config.json"), Bench.JSON.writeValueAsString(config));
        Bench.log("chaos run -> " + out.toAbsolutePath());
        Bench.log("config " + config);

        List<Processes.Server> servers = List.of(new Processes.Server(serverPort, script, out));
        Proxy proxy = new Proxy(proxyPort, "127.0.0.1", serverPort);
        Map<String, String> workerEnv = new LinkedHashMap<>(Map.of("BUILD_ID", "chaos", "CHAOS_SEED", Long.toString(seed)));
        for (String name : List.of("CHAOS_TASK_ERRORS", "CHAOS_TASK_FAILURES")) {
            if (System.getenv(name) != null) {
                workerEnv.put(name, System.getenv(name));
            }
        }
        boolean injecting = workerEnv.containsKey("CHAOS_TASK_ERRORS") || workerEnv.containsKey("CHAOS_TASK_FAILURES");
        Processes.Workers workers = new Processes.Workers(workerCount, "http://127.0.0.1:" + proxyPort + "/api", out,
                workerEnv, "kitchensink.Worker");
        List<String> events = new ArrayList<>();
        Nemesis nemesis = new Nemesis(faults, interval, new Dice(seed), workers, servers, proxy, List.of(), null,
                (type, fault, target) -> {
                    synchronized (events) {
                        events.add(System.currentTimeMillis() + " " + type + " " + fault + " " + target);
                    }
                });

        servers.get(0).ensureUp(Duration.ofMinutes(4));
        workers.startAll();
        Processes.sleepMillis(5_000); // the workers register the definitions

        List<String> executions = new ArrayList<>();
        int failedRounds = 0;
        for (int round = 1; round <= rounds; round++) {
            Path log = out.resolve("driver-" + round + ".log");
            String java = ProcessHandle.current().info().command().orElse("java");
            ProcessBuilder pb = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), "kitchensink.Main");
            pb.environment().put("CONDUCTOR_SERVER_URL", "http://localhost:" + serverPort + "/api");
            pb.environment().put("KITCHENSINK_HOST", "false");
            pb.environment().remove("KITCHENSINK_EXPORT");
            if (injecting) {
                pb.environment().put("KITCHENSINK_KEEP_GOING", "true");
            }
            pb.redirectErrorStream(true).redirectOutput(log.toFile());
            Process driver = pb.start();
            Bench.log("round " + round + ": driver started (pid " + driver.pid() + ")");
            long until = System.currentTimeMillis() + Duration.ofMinutes(40).toMillis();
            Thread chaos = Thread.ofVirtual().name("nemesis").start(() -> {
                // faults until the driver ends: run() stops 10 s before its deadline, so it is
                // given a deadline that moves with the driver
                while (driver.isAlive()) {
                    nemesis.run(Math.min(until, System.currentTimeMillis() + 30_000));
                }
            });
            boolean ended = driver.waitFor(40, TimeUnit.MINUTES);
            if (!ended) {
                driver.destroyForcibly();
            }
            chaos.join();
            nemesis.heal();
            String text = Files.readString(log);
            Matcher m = STARTED.matcher(text);
            while (m.find()) {
                executions.add(m.group(1));
            }
            int status = ended ? driver.exitValue() : -1;
            Bench.log("round " + round + ": driver exit " + status + ", "
                    + text.lines().filter(l -> l.startsWith("  PASS")).count() + " checks passed, "
                    + text.lines().filter(l -> l.startsWith("  FAIL")).count() + " failed");
            text.lines().filter(l -> l.startsWith("  FAIL") || l.startsWith("KITCHENSINK") || l.startsWith("ENDED"))
                    .forEach(Bench::log);
            if (status != 0 && !injecting) {
                failedRounds++;
            }
        }
        workers.stopAll();
        proxy.close();
        Files.writeString(out.resolve("executions.txt"), String.join("\n", executions) + "\n");
        Files.writeString(out.resolve("nemesis.log"), String.join("\n", events) + "\n");
        Map<String, Integer> injected = new LinkedHashMap<>();
        for (String e : events) {
            String[] parts = e.split(" ");
            if (parts[1].equals("start")) {
                injected.merge(parts[2], 1, Integer::sum);
            }
        }
        Bench.log("faults injected " + injected);
        Bench.log("executions " + String.join(" ", executions));
        Bench.log("results in " + out.toAbsolutePath());
        System.exit(failedRounds == 0 ? 0 : 1);
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v;
    }

    private static int intEnv(String name, int fallback) {
        return Integer.parseInt(env(name, Integer.toString(fallback)));
    }
}
