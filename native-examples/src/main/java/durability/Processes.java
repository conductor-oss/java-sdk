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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Starts, kills, pauses and resumes the server and the worker processes for the bench. */
final class Processes {

    private Processes() {}

    static int run(String... command) {
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor();
        } catch (IOException | InterruptedException e) {
            return -1;
        }
    }

    static String output(String... command) {
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor();
            return out.trim();
        } catch (IOException | InterruptedException e) {
            return "";
        }
    }

    static void signal(long pid, String sig) {
        run("kill", "-" + sig, Long.toString(pid));
    }

    static void sleepMillis(long millis) {
        try {
            Thread.sleep(Math.max(0, millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** The Conductor server under test, found by its port and restarted with a script. */
    static final class Server {
        final int port;
        private final String script;
        private final Path logs;
        private final String[] args;
        private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        private volatile Long pausedPid;
        private volatile Process started; // the last server process this bench started
        private int starts;

        /** {@code args} are passed to the script, for a cluster the HTTP port and database port. */
        Server(int port, String script, Path logs, String... args) {
            this.port = port;
            this.script = script;
            this.logs = logs;
            this.args = args;
        }

        Long pid() {
            String out = Processes.output("lsof", "-ti", "tcp:" + port, "-sTCP:LISTEN");
            if (out.isBlank()) {
                return null;
            }
            return Long.parseLong(out.lines().findFirst().orElseThrow().trim());
        }

        boolean healthy() {
            try {
                HttpResponse<String> r = http.send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/health"))
                                .timeout(Duration.ofSeconds(3)).build(),
                        HttpResponse.BodyHandlers.ofString());
                return r.statusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        }

        void kill() {
            Long pid = pid();
            if (pid != null) {
                Processes.signal(pid, "9");
            }
            Process mine = started;
            if (mine != null && mine.isAlive()) {
                mine.destroyForcibly(); // also one still booting, not yet on the port
            }
            for (int i = 0; i < 100 && pid() != null; i++) {
                sleepMillis(100);
            }
        }

        void start() throws IOException {
            if (script == null || script.isBlank()) {
                throw new IllegalStateException("BENCH_SERVER_SCRIPT is not set; cannot start the server");
            }
            Process mine = started;
            if (mine != null && mine.isAlive()) {
                return; // still booting: a second instance would share the database headless
            }
            File log = logs.resolve("server-" + port + "-start-" + (++starts) + ".log").toFile();
            List<String> command = new ArrayList<>(List.of("bash", script));
            command.addAll(List.of(args));
            started = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(log)).start();
        }

        void pause() {
            Long pid = pid();
            if (pid != null) {
                Processes.signal(pid, "STOP");
                pausedPid = pid;
            }
        }

        void resume() {
            Long pid = pausedPid;
            if (pid != null) {
                Processes.signal(pid, "CONT");
                pausedPid = null;
            }
        }

        /** Resumes the server if paused, starts it if it is not running, and waits until it is healthy. */
        void ensureUp(Duration timeout) throws IOException {
            resume();
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            if (!healthy() && pid() == null) {
                start(); // no-op while a server this bench started is still booting
            }
            while (!healthy()) {
                if (System.currentTimeMillis() > deadline) {
                    throw new IllegalStateException("server on port " + port + " did not become healthy");
                }
                sleepMillis(500);
            }
        }
    }

    /** The Postgres container the cluster shares, for database faults. */
    static final class Database {
        private final String container;
        private final int port;
        private volatile boolean paused;

        Database(String container, int port) {
            this.container = container;
            this.port = port;
        }

        void pause() {
            Processes.run("docker", "pause", container);
            paused = true;
        }

        void resume() {
            if (paused) {
                Processes.run("docker", "unpause", container);
                paused = false;
            }
        }

        /** SIGKILL the database; {@link #start} brings it back with its data. */
        void kill() {
            Processes.run("docker", "kill", container);
        }

        void start() {
            Processes.run("docker", "start", container);
        }

        boolean ready() {
            try (java.net.Socket s = new java.net.Socket()) {
                s.connect(new java.net.InetSocketAddress("127.0.0.1", port), 1000);
                return Processes.run("docker", "exec", container, "pg_isready", "-q") == 0;
            } catch (IOException e) {
                return false;
            }
        }

        void ensureUp(Duration timeout) {
            resume();
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            if (!ready()) {
                start();
            }
            while (!ready()) {
                if (System.currentTimeMillis() > deadline) {
                    throw new IllegalStateException("database " + container + " did not come back");
                }
                sleepMillis(500);
            }
        }
    }

    /** The worker processes, each a {@link BenchWorker} with its own worker id. */
    static final class Workers {
        private final List<Slot> slots = new ArrayList<>();
        private final String serverUrl;
        private final Path out;
        private final Map<String, String> extraEnv;
        private final String mainClass;
        private final AtomicLong generation = new AtomicLong();

        static final class Slot {
            final int index;
            volatile String workerId;
            volatile Process process;
            volatile boolean paused;

            Slot(int index) {
                this.index = index;
            }

            boolean alive() {
                return process != null && process.isAlive();
            }
        }

        Workers(int count, String serverUrl, Path out, Map<String, String> extraEnv) {
            this(count, serverUrl, out, extraEnv, "durability.BenchWorker");
        }

        /** Worker processes running {@code mainClass}, each with its own worker id. */
        Workers(int count, String serverUrl, Path out, Map<String, String> extraEnv, String mainClass) {
            this.mainClass = mainClass;
            for (int i = 0; i < count; i++) {
                slots.add(new Slot(i));
            }
            this.serverUrl = serverUrl;
            this.out = out;
            this.extraEnv = extraEnv;
        }

        List<Slot> slots() {
            return slots;
        }

        void startAll() throws IOException {
            for (Slot s : slots) {
                start(s, "w" + s.index);
            }
        }

        void start(Slot slot, String workerId) throws IOException {
            String java = ProcessHandle.current().info().command().orElse("java");
            ProcessBuilder pb = new ProcessBuilder(java, "-Xmx384m", "-cp", System.getProperty("java.class.path"),
                    mainClass);
            pb.environment().putAll(extraEnv);
            pb.environment().put("CONDUCTOR_SERVER_URL", serverUrl);
            pb.environment().put("CONDUCTOR_WORKER_ID", workerId);
            pb.environment().put("LEDGER_DIR", out.resolve("ledger").toString());
            pb.environment().putIfAbsent("BUILD_ID", "bench");
            // production settings: dev mode stretches the continuation response timeout to an hour
            pb.environment().remove("CONDUCTOR_DEV");
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(out.resolve("worker-" + workerId + ".log").toFile()));
            slot.workerId = workerId;
            slot.paused = false;
            slot.process = pb.start();
        }

        /** A new worker id, as if a new host replaced a dead one. */
        String freshId(Slot slot) {
            return "w" + slot.index + "-g" + generation.incrementAndGet();
        }

        void kill(Slot slot) throws InterruptedException {
            Process p = slot.process;
            if (p != null) {
                if (slot.paused) {
                    Processes.signal(p.pid(), "CONT");
                    slot.paused = false;
                }
                p.destroyForcibly(); // SIGKILL
                p.waitFor(10, TimeUnit.SECONDS);
            }
        }

        void pause(Slot slot) {
            if (slot.alive()) {
                Processes.signal(slot.process.pid(), "STOP");
                slot.paused = true;
            }
        }

        void resume(Slot slot) {
            if (slot.paused && slot.process != null) {
                Processes.signal(slot.process.pid(), "CONT");
                slot.paused = false;
            }
        }

        /** Resumes paused workers and restarts dead ones under their last id. */
        void ensureAll() throws IOException {
            for (Slot s : slots) {
                resume(s);
                if (!s.alive()) {
                    start(s, s.workerId);
                }
            }
        }

        void stopAll() throws InterruptedException {
            for (Slot s : slots) {
                kill(s);
            }
        }
    }

    /** Plain HTTP reads for the final phase, independent of the SDK under test. */
    static final class Api {
        private final String base;
        private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

        Api(String base) {
            this.base = base;
        }

        /** The body of a GET, or null on 404; retries other failures for up to a minute. */
        String get(String path) {
            long deadline = System.currentTimeMillis() + 60_000;
            while (true) {
                try {
                    HttpResponse<String> r = http.send(
                            HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20)).build(),
                            HttpResponse.BodyHandlers.ofString());
                    if (r.statusCode() == 404) {
                        return null;
                    }
                    if (r.statusCode() == 200) {
                        return r.body();
                    }
                } catch (Exception ignored) {
                    // retried below
                }
                if (System.currentTimeMillis() > deadline) {
                    throw new IllegalStateException("GET " + path + " kept failing");
                }
                sleepMillis(1000);
            }
        }
    }

    /** Deterministic randomness for the fault schedule, from {@code BENCH_SEED}. */
    static final class Dice {
        private final Random random;

        Dice(long seed) {
            this.random = new Random(seed);
        }

        synchronized int between(int lo, int hi) {
            return lo + random.nextInt(Math.max(1, hi - lo + 1));
        }

        synchronized boolean chance(double p) {
            return random.nextDouble() < p;
        }

        synchronized <T> T pick(List<T> items) {
            return items.get(random.nextInt(items.size()));
        }

        synchronized double exponential(double mean) {
            return -mean * Math.log(1 - random.nextDouble());
        }
    }

    static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    static void writeString(Path file, String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }
}
