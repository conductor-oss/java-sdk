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
package io.orkes.conductor.harness.e2e;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.common.run.Workflow;

import io.orkes.conductor.client.ApiClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Functional scenarios for adaptive concurrency against a real server and one worker process: depth-weighted
 * shares, floor rotation, starvation, scaling up and down, CPU pressure and heap pressure. Each scenario loads the
 * queues before the worker starts polling, so the controller sees a known backlog, and checks the outcome against
 * the design's rules.
 */
public class E2eFunctionalDriver {

    private static final Logger log = LoggerFactory.getLogger(E2eFunctionalDriver.class);

    private static final String OWNER = "java-sdk-e2e@conductor.io";
    private static final List<String> TASKS = List.of("fn_a", "fn_b", "fn_c", "fn_d");

    private final String scenario;
    private final WorkflowClient workflowClient;
    private final MetadataClient metadataClient;
    private final TaskClient taskClient;
    private final Map<String, String> domains;
    private final E2eSampler sampler;
    private final Path logDir;
    private final int maxProcess = E2eConfig.envInt("E2E_MAX_PROCESS_CONCURRENCY", 256);
    /** The SDK clamps each worker's maximum to the process ceiling. */
    private final int maxWorker = Math.min(maxProcess, E2eConfig.envInt("E2E_MAX_WORKER_CONCURRENCY", 64));
    private final ExecutorService starters = Executors.newFixedThreadPool(32);
    private final ConcurrentLinkedQueue<String> started = new ConcurrentLinkedQueue<>();
    private final List<Map<String, Object>> checks = new ArrayList<>();
    private final Map<String, Object> observations = new LinkedHashMap<>();
    private final Random random = new Random(7);
    private long goMillis;

    E2eFunctionalDriver(String scenario, ConductorClient client) {
        this.scenario = scenario;
        this.workflowClient = new WorkflowClient(client);
        this.metadataClient = new MetadataClient(client);
        this.taskClient = new TaskClient(client);
        this.domains = E2eConfig.envStringMap("E2E_TASK_DOMAINS", "");
        this.logDir = Path.of(E2eConfig.env("E2E_LOG_DIR", "build/e2e/logs"));
        List<String> queueNames = TASKS.stream().map(this::queueName).toList();
        this.sampler = new E2eSampler(taskClient, queueNames, null,
                List.of(E2eConfig.env("E2E_WORKER_METRICS", "http://localhost:9993")));
    }

    public static void main(String[] args) throws Exception {
        ConductorClient client = ApiClient.builder().useEnvVariables(true).readTimeout(30_000).build();
        String scenario = E2eConfig.env("E2E_SCENARIO", "weights");
        Path outDir = Path.of(E2eConfig.env("E2E_OUT_DIR", "build/e2e/" + scenario));
        Files.createDirectories(outDir);
        E2eFunctionalDriver driver = new E2eFunctionalDriver(scenario, client);
        boolean passed = driver.run(outDir);
        System.exit(passed ? 0 : 1);
    }

    boolean run(Path outDir) throws Exception {
        registerMetadata();
        sampler.start();
        sampler.setPhase("load");
        switch (scenario) {
            case "weights" -> weights();
            case "rotation" -> rotation();
            case "starvation" -> starvation();
            case "scaling" -> scaling();
            case "warmstart" -> warmStart();
            case "cpu" -> cpu();
            case "heap" -> heap();
            default -> throw new IllegalArgumentException("Unknown scenario " + scenario);
        }
        sampler.setPhase("final");
        Thread.sleep(3_000);
        sampler.stop();
        Map<String, Workflow> workflows = fetchWorkflows();
        commonChecks(workflows);
        sampler.writeCsv(outDir.resolve("timeseries.csv"));
        return writeReport(outDir);
    }

    // ----- Scenarios -----

    /** P = 16 and backlogs of 600/300/150/30: shares, executions and throughput must follow backlog. */
    private void weights() throws Exception {
        Map<String, Integer> backlog = Map.of("fn_a", 600, "fn_b", 300, "fn_c", 150, "fn_d", 30);
        backlog.forEach((task, count) -> preload(task, count, Map.of("sleepMs", 1000)));
        go();
        waitForDrain(600);

        List<E2eSampler.Sample> contended = after(3_000).stream()
                .filter(s -> TASKS.stream().mapToDouble(t -> depth(s, t)).sum() > 0)
                .toList();
        int sharesMatch = 0;
        int executionsMatch = 0;
        int fullSamples = 0;
        int fullAssigned = 0;
        int counted = 0;
        for (E2eSampler.Sample s : contended) {
            int target = (int) s.get("w0.processTarget");
            long[] backlogs = new long[TASKS.size()];
            long[] demands = new long[TASKS.size()];
            long totalDemand = 0;
            for (int i = 0; i < TASKS.size(); i++) {
                backlogs[i] = (long) (depth(s, TASKS.get(i)) + get(s, TASKS.get(i), "held"));
                demands[i] = Math.min(maxWorker, backlogs[i]);
                totalDemand += demands[i];
            }
            int[] expected = expectedShares(target, backlogs, demands);
            boolean shares = true;
            boolean executions = true;
            int assigned = 0;
            for (int i = 0; i < TASKS.size(); i++) {
                double actual = get(s, TASKS.get(i), "target");
                shares &= Math.abs(actual - expected[i]) <= 1;
                executions &= Math.abs(get(s, TASKS.get(i), "inflight") - actual) <= 1;
                assigned += (int) actual;
            }
            counted++;
            sharesMatch += shares ? 1 : 0;
            executionsMatch += executions ? 1 : 0;
            if (totalDemand >= target) {
                fullSamples++;
                fullAssigned += assigned >= target ? 1 : 0;
            }
        }
        check("shares follow backlog", counted > 0 && sharesMatch >= 0.9 * counted,
                sharesMatch + "/" + counted + " samples within one slot of the water-filled shares");
        check("executions follow shares", counted > 0 && executionsMatch >= 0.85 * counted,
                executionsMatch + "/" + counted + " samples with live executions within one of the share");
        check("no slot left unassigned", fullSamples > 0 && fullAssigned >= 0.95 * fullSamples,
                fullAssigned + "/" + fullSamples + " samples with demand >= target assign the whole target");

        // Throughput while all four queues are backlogged must split like the shares.
        long contentionEnd = contended.stream().filter(s -> depth(s, "fn_d") == 0).findFirst()
                .map(E2eSampler.Sample::epochMillis).orElse(Long.MAX_VALUE);
        List<E2eAnalysis.Execution> window = executions().stream()
                .filter(e -> e.start() >= goMillis + 5_000 && e.start() < contentionEnd)
                .toList();
        int[] expected = expectedShares(maxProcess, new long[] {600, 300, 150, 30},
                new long[] {maxWorker, maxWorker, maxWorker, 30});
        Map<String, Long> starts = window.stream()
                .collect(Collectors.groupingBy(E2eAnalysis.Execution::taskType, Collectors.counting()));
        Map<String, Object> slotEquivalent = new LinkedHashMap<>();
        boolean proportional = window.size() > 50;
        for (int i = 0; i < TASKS.size(); i++) {
            double slots = maxProcess * (double) starts.getOrDefault(TASKS.get(i), 0L) / Math.max(1, window.size());
            slotEquivalent.put(TASKS.get(i), round(slots) + " (expected " + expected[i] + ")");
            proportional &= Math.abs(slots - expected[i]) <= 1;
        }
        check("throughput splits by backlog", proportional,
                window.size() + " starts while all queues were backlogged; slot-equivalents " + slotEquivalent);
        checkPeak(maxProcess);
    }

    /** P = 2 with four backlogged queues: floors exceed the target, so queues must take turns. */
    private void rotation() throws Exception {
        for (String task : TASKS) {
            preload(task, 40, Map.of("sleepMs", 1000));
        }
        go();
        waitForDrain(600);
        checkProgress(15_000);
        checkPeak(maxProcess);
        Map<String, Long> finish = executions().stream().collect(Collectors.groupingBy(
                E2eAnalysis.Execution::taskType, Collectors.reducing(0L, E2eAnalysis.Execution::end, Math::max)));
        long first = Collections.min(finish.values()) - goMillis;
        long last = Collections.max(finish.values()) - goMillis;
        check("equal backlogs finish together", last <= first * 1.25,
                "queues finished between " + first / 1000 + "s and " + last / 1000 + "s after start");
    }

    /** P = 4: three deep queues plus a trickle of domain-routed tasks that must neither wait nor fall back. */
    private void starvation() throws Exception {
        for (String task : List.of("fn_a", "fn_b", "fn_c")) {
            preload(task, 100, Map.of("sleepMs", 2000));
        }
        go();
        // Routing to a domain needs a poll from it first, as in fixed mode; start the trickle once there is one.
        while (taskClient.getPollData("fn_d").stream().noneMatch(p -> domains.get("fn_d").equals(p.getDomain()))) {
            Thread.sleep(200);
        }
        long end = System.currentTimeMillis() + 90_000;
        while (System.currentTimeMillis() < end) {
            start("fn_d", Map.of("sleepMs", 200));
            Thread.sleep(3_000);
        }
        waitForDrain(600);
        checkProgress(15_000);
        List<Task> trickle = tasks("fn_d");
        long fellBack = trickle.stream().filter(t -> !domains.get("fn_d").equals(t.getDomain())).count();
        check("trickle stays in its domain", !trickle.isEmpty() && fellBack == 0,
                trickle.size() + " trickle tasks, " + fellBack + " routed outside domain " + domains.get("fn_d"));
        List<Long> waits = trickle.stream().filter(t -> t.getStartTime() > 0)
                .map(t -> t.getStartTime() - t.getScheduledTime()).sorted().toList();
        long maxWait = waits.isEmpty() ? Long.MAX_VALUE : waits.get(waits.size() - 1);
        check("trickle is not starved", waits.size() == trickle.size() && maxWait <= 5_000,
                "trickle queue wait p50 " + (waits.isEmpty() ? "-" : waits.get(waits.size() / 2)) + " ms, max "
                        + maxWait + " ms (limit 5000)");
        checkPeak(maxProcess);
    }

    /** A deep I/O backlog: the target grows by the step rule to the ceiling, then decays once the backlog clears. */
    private void scaling() throws Exception {
        preload("fn_a", 1500, Map.of("sleepMs", 2000));
        go();
        waitForQueuesEmpty(600);
        long drainedAt = System.currentTimeMillis();
        sampler.setPhase("idle");
        waitForIdle(30);
        Thread.sleep(E2eConfig.envInt("E2E_IDLE_SECONDS", 240) * 1000L);

        List<E2eSampler.Sample> busy = after(0).stream().filter(s -> s.epochMillis() < drainedAt).toList();
        List<String> badSteps = new ArrayList<>();
        double previous = Double.NaN;
        long previousChange = goMillis;
        int steps = 0;
        for (E2eSampler.Sample s : busy) {
            double target = s.get("w0.processTarget");
            if (!Double.isNaN(previous) && target > previous) {
                steps++;
                double limit = Math.min(16, Math.max(1, Math.ceil(0.2 * previous)));
                long gap = s.epochMillis() - previousChange;
                if (target - previous > limit || gap < 3_500) {
                    badSteps.add((int) previous + "->" + (int) target + " after " + gap + "ms");
                }
            }
            if (!Double.isNaN(previous) && target != previous) {
                previousChange = s.epochMillis();
            }
            previous = target;
        }
        double peak = busy.stream().mapToDouble(s -> s.get("w0.processTarget")).filter(v -> !Double.isNaN(v)).max()
                .orElse(0);
        check("growth follows the step rule", steps > 0 && badSteps.isEmpty(),
                steps + " growth steps" + (badSteps.isEmpty() ? "" : ", violations " + badSteps));
        int ceiling = Math.min(maxProcess, maxWorker);
        check("target reaches the ceiling", peak >= ceiling, "peak " + peak + " (ceiling " + ceiling + ")");
        long threadsShort = busy.stream().filter(s -> get(s, "fn_a", "threads") < get(s, "fn_a", "inflight")).count();
        long threadsOver = busy.stream()
                .filter(s -> get(s, "fn_a", "threads") > Math.max(s.get("w0.processTarget"), get(s, "fn_a", "held")) + 1)
                .count();
        check("threads track the target", threadsShort == 0 && threadsOver <= busy.size() / 20,
                threadsShort + " samples with fewer threads than executions, " + threadsOver
                        + " with more threads than max(target, held) + 1, of " + busy.size());
        double usedAtPeak = busy.stream().filter(s -> s.get("w0.processTarget") >= ceiling)
                .mapToDouble(s -> get(s, "fn_a", "inflight")).max().orElse(0);
        check("capacity is used", usedAtPeak >= 0.9 * ceiling, "max executions at the ceiling " + usedAtPeak);

        List<E2eSampler.Sample> idle = samplesIn("idle");
        List<String> badDecay = new ArrayList<>();
        previous = Double.NaN;
        previousChange = drainedAt;
        for (E2eSampler.Sample s : idle) {
            double target = s.get("w0.processTarget");
            if (!Double.isNaN(previous) && target < previous) {
                long gap = s.epochMillis() - previousChange;
                if (target < Math.ceil(previous / 2) || gap < 25_000) {
                    badDecay.add((int) previous + "->" + (int) target + " after " + gap + "ms");
                }
                previousChange = s.epochMillis();
            }
            previous = target;
        }
        E2eSampler.Sample last = idle.get(idle.size() - 1);
        // Idle, the target keeps one slot per queue for its standby poll.
        int idleTarget = standbyEnabled() ? TASKS.size() : 1;
        check("target decays when idle", last.get("w0.processTarget") <= idleTarget && badDecay.isEmpty(),
                "end of idle target " + last.get("w0.processTarget") + " (expected at most " + idleTarget + ")"
                        + (badDecay.isEmpty() ? "" : ", violations " + badDecay));
        double threadsLeft = TASKS.stream().mapToDouble(t -> get(last, t, "threads")).sum();
        check("idle threads are released", threadsLeft <= TASKS.size(),
                "execution threads at the end " + threadsLeft + " across " + TASKS.size() + " queues");
        observations.put("targetSeries", series(Stream.concat(busy.stream(), idle.stream()).toList(),
                s -> s.get("w0.processTarget")));
    }

    private static boolean standbyEnabled() {
        return E2eConfig.envInt("E2E_STANDBY_POLL_TIMEOUT_MS", 1000) > 0;
    }

    /** A configured start and minimum: no ramp from one slot, decay stops at the minimum, bursts start warm. */
    private void warmStart() throws Exception {
        int initial = E2eConfig.envInt("E2E_INITIAL_PROCESS_CONCURRENCY", 32);
        int minimum = E2eConfig.envInt("E2E_MIN_PROCESS_CONCURRENCY", 16);
        preload("fn_a", 1500, Map.of("sleepMs", 2000));
        go();
        waitForQueuesEmpty(600);
        long drainedAt = System.currentTimeMillis();
        sampler.setPhase("idle");
        waitForIdle(30);
        Thread.sleep(E2eConfig.envInt("E2E_IDLE_SECONDS", 150) * 1000L);

        List<E2eSampler.Sample> busy = after(0).stream().filter(s -> s.epochMillis() < drainedAt).toList();
        double first = busy.stream().mapToDouble(s -> s.get("w0.processTarget")).filter(v -> !Double.isNaN(v))
                .findFirst().orElse(Double.NaN);
        check("starts at the configured target", first >= initial, "first target " + first + " (configured "
                + initial + ")");
        long reachedCeiling = busy.stream().filter(s -> s.get("w0.processTarget") >= Math.min(maxProcess, maxWorker))
                .map(E2eSampler.Sample::epochMillis).findFirst().orElse(Long.MAX_VALUE);
        check("reaches the ceiling", reachedCeiling != Long.MAX_VALUE,
                "ceiling reached " + (reachedCeiling == Long.MAX_VALUE ? "never" : (reachedCeiling - goMillis) / 1000
                        + "s after start"));
        List<E2eSampler.Sample> idle = samplesIn("idle");
        double lowest = idle.stream().mapToDouble(s -> s.get("w0.processTarget")).filter(v -> !Double.isNaN(v)).min()
                .orElse(Double.NaN);
        double end = idle.get(idle.size() - 1).get("w0.processTarget");
        check("decay stops at the minimum", lowest >= minimum && end == minimum,
                "lowest idle target " + lowest + ", end of idle " + end + " (minimum " + minimum + ")");
        double threadsLeft = TASKS.stream().mapToDouble(t -> get(idle.get(idle.size() - 1), t, "threads")).sum();
        check("idle threads are released", threadsLeft <= TASKS.size(),
                "execution threads at the end of idle " + threadsLeft);

        // A second burst after idle starts with the minimum already available.
        sampler.setPhase("burst");
        long burstAt = System.currentTimeMillis();
        preload("fn_a", 200, Map.of("sleepMs", 2000));
        waitForDrain(300);
        long started = executions().stream()
                .filter(e -> e.start() >= burstAt && e.start() <= burstAt + 3_000).count();
        check("burst after idle starts warm", started >= minimum,
                started + " tasks started within 3s of the burst (minimum " + minimum + ")");
        observations.put("targetSeries", series(after(0), s -> s.get("w0.processTarget")));
    }

    /** One CPU, CPU-bound tasks, starting at P = 8: reductions of 25% at most every 15 s and no growth meanwhile. */
    private void cpu() throws Exception {
        preload("fn_a", 300, Map.of("cpuMs", 250));
        go();
        waitForDrain(900);
        List<E2eSampler.Sample> busy = after(0).stream().filter(s -> depth(s, "fn_a") > 0).toList();
        List<Double> reductions = new ArrayList<>();
        List<String> bad = new ArrayList<>();
        double previous = Double.NaN;
        long lastReduction = 0;
        for (int i = 0; i < busy.size(); i++) {
            E2eSampler.Sample s = busy.get(i);
            double target = s.get("w0.processTarget");
            if (!Double.isNaN(previous) && target < previous) {
                reductions.add(target);
                double expected = previous * 0.75;
                long gap = lastReduction == 0 ? Long.MAX_VALUE : s.epochMillis() - lastReduction;
                if (Math.abs(target - expected) > 1 || gap < 14_000) {
                    bad.add((int) previous + "->" + (int) target + " after " + gap + "ms");
                }
                lastReduction = s.epochMillis();
            } else if (!Double.isNaN(previous) && target > previous && i >= 3
                    && busy.subList(i - 3, i).stream().allMatch(p -> p.get("w0.cpu") >= 0.9)) {
                bad.add("growth " + (int) previous + "->" + (int) target + " while CPU was saturated");
            }
            previous = target;
        }
        check("CPU pressure reduces the target", !reductions.isEmpty() && bad.isEmpty(),
                reductions.size() + " reductions to " + reductions + (bad.isEmpty() ? "" : ", violations " + bad));
        double settled = busy.isEmpty() ? Double.NaN : busy.get(busy.size() - 1).get("w0.processTarget");
        check("target settles near the core count", settled <= 2, "target at the end of the backlog " + settled);
        List<E2eAnalysis.Execution> runs = executions();
        double seconds = (runs.stream().mapToLong(E2eAnalysis.Execution::end).max().orElse(0)
                - runs.stream().mapToLong(E2eAnalysis.Execution::start).min().orElse(0)) / 1000.0;
        double throughput = runs.size() / Math.max(1, seconds);
        check("CPU stays productive", throughput >= 0.7 * 4, round(throughput) + " tasks/s (one core, 250 ms "
                + "of CPU each: ideal 4/s)");
    }

    /** Tasks that hold large payloads in a small heap: no OOM, and no polling while admission is paused. */
    private void heap() throws Exception {
        for (int i = 0; i < 300; i++) {
            int allocMb = (int) Math.min(30, Math.round(6 * Math.exp(0.5 * random.nextGaussian())));
            start("fn_a", Map.of("allocMb", allocMb, "sleepMs", 3000));
        }
        awaitStarts();
        go();
        waitForDrain(900);
        List<E2eSampler.Sample> run = after(0);
        long down = run.stream().filter(s -> s.get("w0.up") != 1).count();
        check("worker survives heap pressure", down == 0, down + " samples with the worker unreachable");
        // Rule one: nothing polls without an open slot, so nothing polls while admission is paused.
        List<E2eAnalysis.Execution> runs = executions();
        int pausedSamples = 0;
        long startsWhilePaused = 0;
        double pollsWhilePaused = 0;
        for (int i = 1; i < run.size(); i++) {
            E2eSampler.Sample before = run.get(i - 1);
            E2eSampler.Sample now = run.get(i);
            if (before.get("w0.paused") == 1 && now.get("w0.paused") == 1) {
                pausedSamples++;
                pollsWhilePaused += get(now, "fn_a", "polls") - get(before, "fn_a", "polls");
                startsWhilePaused += runs.stream()
                        .filter(e -> e.start() > before.epochMillis() && e.start() <= now.epochMillis()).count();
            }
        }
        check("no polling while paused", startsWhilePaused == 0 && pollsWhilePaused == 0,
                pausedSamples + " paused seconds, " + (int) pollsWhilePaused + " polls, " + startsWhilePaused
                        + " task starts");
        observations.put("pausedSeconds", pausedSamples);
        observations.put("peakTarget", run.stream().mapToDouble(s -> s.get("w0.processTarget")).filter(v -> !Double.isNaN(v)).max().orElse(0));
        observations.put("peakHeapMb", run.stream().mapToDouble(s -> s.get("w0.heapUsedMb")).max().orElse(0));
        observations.put("heapMaxMb", run.get(0).get("w0.heapMaxMb"));
        observations.put("reasons", reasons(run.get(run.size() - 1)));
    }

    // ----- Checks shared by scenarios -----

    private void commonChecks(Map<String, Workflow> workflows) throws IOException {
        long completed = workflows.values().stream()
                .filter(w -> w.getStatus() == Workflow.WorkflowStatus.COMPLETED).count();
        check("all workflows completed", completed == started.size(), completed + "/" + started.size());
        Map<String, Long> terminal = executions().stream().filter(e -> !"IN_PROGRESS".equals(e.status()))
                .collect(Collectors.groupingBy(E2eAnalysis.Execution::taskId, Collectors.counting()));
        long duplicates = terminal.values().stream().filter(count -> count > 1).count();
        check("no duplicate executions", duplicates == 0, duplicates + " tasks executed more than once");
        E2eSampler.Sample last = sampler.latest();
        // Each idle queue may hold one slot for its standby poll.
        int standbySlots = standbyEnabled() ? TASKS.size() : 0;
        check("slots released", last.get("w0.processHeld") <= standbySlots && last.get("w0.inflight") == 0,
                "held " + last.get("w0.processHeld") + " (standby at most " + standbySlots + "), executing "
                        + last.get("w0.inflight"));
    }

    private void checkPeak(int limit) throws IOException {
        List<long[]> events = new ArrayList<>();
        for (E2eAnalysis.Execution e : executions()) {
            events.add(new long[] {e.start(), 1});
            events.add(new long[] {e.end(), -1});
        }
        events.sort((a, b) -> a[0] != b[0] ? Long.compare(a[0], b[0]) : Long.compare(a[1], b[1]));
        int current = 0;
        int peak = 0;
        for (long[] event : events) {
            current += (int) event[1];
            peak = Math.max(peak, current);
        }
        check("process concurrency within target", peak <= limit, "peak " + peak + " concurrent (limit " + limit
                + ")");
    }

    /** Every queue with remaining backlog starts at least one task in every window. */
    private void checkProgress(long windowMillis) throws IOException {
        Map<String, List<E2eAnalysis.Execution>> byType = executions().stream()
                .collect(Collectors.groupingBy(E2eAnalysis.Execution::taskType));
        List<String> gaps = new ArrayList<>();
        byType.forEach((type, runs) -> {
            List<Long> startTimes = runs.stream().map(E2eAnalysis.Execution::start).sorted().toList();
            long previous = goMillis;
            for (long start : startTimes) {
                if (start - previous > windowMillis) {
                    gaps.add(type + " idle " + (start - previous) / 1000 + "s");
                }
                previous = start;
            }
        });
        check("every backlogged queue progresses", gaps.isEmpty(),
                gaps.isEmpty() ? "no queue went " + windowMillis / 1000 + "s without starting a task" : gaps);
    }

    /** The spec's allocation: shares by backlog, water-filled against demand, floors, largest remainder. */
    static int[] expectedShares(int target, long[] backlogs, long[] demands) {
        int n = backlogs.length;
        int[] shares = new int[n];
        long total = 0;
        for (long demand : demands) {
            total += demand;
        }
        if (total <= target) {
            for (int i = 0; i < n; i++) {
                shares[i] = (int) demands[i];
            }
            return shares;
        }
        boolean[] fixed = new boolean[n];
        double[] quotas = new double[n];
        long remaining = target;
        boolean changed = true;
        while (changed) {
            changed = false;
            long weight = 0;
            for (int i = 0; i < n; i++) {
                weight += fixed[i] ? 0 : backlogs[i];
            }
            for (int i = 0; i < n; i++) {
                if (fixed[i] || demands[i] == 0) {
                    continue;
                }
                quotas[i] = (double) remaining * backlogs[i] / weight;
                if (quotas[i] >= demands[i]) {
                    fixed[i] = true;
                    shares[i] = (int) demands[i];
                    remaining -= demands[i];
                    changed = true;
                }
            }
        }
        long left = remaining;
        List<Integer> open = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!fixed[i] && demands[i] > 0) {
                shares[i] = (int) Math.floor(quotas[i]);
                left -= shares[i];
                open.add(i);
            }
        }
        for (int i : open) {
            if (left > 0 && shares[i] == 0) {
                shares[i] = 1;
                left--;
            }
        }
        open.sort((a, b) -> Double.compare(quotas[b] - Math.floor(quotas[b]), quotas[a] - Math.floor(quotas[a])));
        for (int i : open) {
            if (left > 0 && shares[i] == Math.floor(quotas[i]) && shares[i] < demands[i]) {
                shares[i]++;
                left--;
            }
        }
        for (int i = 0; i < n; i++) {
            shares[i] = demands[i] > 0 ? Math.max(1, shares[i]) : 0;
        }
        return shares;
    }

    // ----- Load -----

    private void preload(String task, int count, Map<String, Object> input) {
        for (int i = 0; i < count; i++) {
            start(task, input);
        }
        awaitStarts();
    }

    private void start(String task, Map<String, Object> input) {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(task + "_wf");
        request.setVersion(1);
        request.setInput(new HashMap<>(input));
        if (domains.containsKey(task)) {
            request.setTaskToDomain(Map.of(task, domains.get(task) + ",NO_DOMAIN"));
        }
        starters.submit(() -> {
            try {
                started.add(workflowClient.startWorkflow(request));
            } catch (Exception e) {
                log.error("Failed to start {}: {}", task, e.getMessage());
            }
        });
    }

    private void awaitStarts() {
        try {
            starters.submit(() -> { }).get();
            // A no-op only proves one thread is free; wait until the backlog of starts is gone.
            while (((java.util.concurrent.ThreadPoolExecutor) starters).getActiveCount() > 0
                    || !((java.util.concurrent.ThreadPoolExecutor) starters).getQueue().isEmpty()) {
                Thread.sleep(100);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void go() throws Exception {
        Thread.sleep(2_000);
        log.info("Loaded {} workflows; depths {}", started.size(), sampler.latest() == null ? "-"
                : TASKS.stream().collect(Collectors.toMap(t -> t, t -> depth(sampler.latest(), t))));
        goMillis = System.currentTimeMillis();
        sampler.setPhase("run");
        Files.writeString(logDir.resolve("go"), Long.toString(goMillis));
    }

    private void waitForDrain(int timeoutSeconds) throws InterruptedException {
        waitForQueuesEmpty(timeoutSeconds);
        waitForIdle(60);
    }

    private void waitForQueuesEmpty(int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(1_000);
            E2eSampler.Sample s = sampler.latest();
            if (s != null && s.epochMillis() > goMillis + 2_000
                    && TASKS.stream().allMatch(t -> depth(s, t) == 0)) {
                return;
            }
        }
        log.warn("Queues not empty after {}s", timeoutSeconds);
    }

    private void waitForIdle(int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(1_000);
            E2eSampler.Sample s = sampler.latest();
            if (s != null && s.get("w0.inflight") == 0 && TASKS.stream().allMatch(t -> depth(s, t) == 0)) {
                return;
            }
        }
    }

    private Map<String, Workflow> fetchWorkflows() throws Exception {
        Map<String, Workflow> workflows = new HashMap<>();
        List<Future<Workflow>> futures = new ArrayList<>();
        ExecutorService fetchers = Executors.newFixedThreadPool(16);
        for (String id : started) {
            futures.add(fetchers.submit(() -> workflowClient.getWorkflow(id, true)));
        }
        for (Future<Workflow> future : futures) {
            Workflow workflow = future.get();
            workflows.put(workflow.getWorkflowId(), workflow);
        }
        fetchers.shutdown();
        starters.shutdown();
        this.workflows = workflows;
        return workflows;
    }

    private Map<String, Workflow> workflows = Map.of();

    private List<Task> tasks(String taskType) {
        try {
            if (workflows.isEmpty()) {
                fetchWorkflows();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return workflows.values().stream().flatMap(w -> w.getTasks().stream())
                .filter(t -> taskType.equals(t.getTaskDefName())).toList();
    }

    private void registerMetadata() {
        List<TaskDef> taskDefs = new ArrayList<>();
        List<WorkflowDef> workflowDefs = new ArrayList<>();
        for (String task : TASKS) {
            TaskDef taskDef = new TaskDef(task);
            taskDef.setOwnerEmail(OWNER);
            taskDef.setRetryCount(0);
            taskDef.setTimeoutSeconds(3600);
            taskDef.setResponseTimeoutSeconds(600);
            taskDefs.add(taskDef);

            WorkflowTask workflowTask = new WorkflowTask();
            workflowTask.setName(task);
            workflowTask.setTaskReferenceName(task);
            workflowTask.setType(TaskType.SIMPLE.name());
            workflowTask.setInputParameters(new HashMap<>(Map.of(
                    "sleepMs", "${workflow.input.sleepMs}",
                    "cpuMs", "${workflow.input.cpuMs}",
                    "allocMb", "${workflow.input.allocMb}")));
            WorkflowDef def = new WorkflowDef();
            def.setName(task + "_wf");
            def.setVersion(1);
            def.setOwnerEmail(OWNER);
            def.setTasks(List.of(workflowTask));
            workflowDefs.add(def);
        }
        metadataClient.registerTaskDefs(taskDefs);
        metadataClient.updateWorkflowDefs(workflowDefs);
    }

    // ----- Helpers -----

    private String queueName(String task) {
        return domains.containsKey(task) ? domains.get(task) + ":" + task : task;
    }

    private double depth(E2eSampler.Sample s, String task) {
        double value = s.get("depth." + queueName(task));
        return Double.isNaN(value) ? 0 : value;
    }

    private static double get(E2eSampler.Sample s, String task, String field) {
        double value = s.get("w0." + task + "." + field);
        return Double.isNaN(value) ? 0 : value;
    }

    private List<E2eSampler.Sample> after(long offsetMillis) {
        return sampler.samples().stream().filter(s -> s.epochMillis() >= goMillis + offsetMillis)
                .filter(s -> !"final".equals(s.phase())).toList();
    }

    private List<E2eSampler.Sample> samplesIn(String phase) {
        return sampler.samples().stream().filter(s -> phase.equals(s.phase())).toList();
    }

    private List<E2eAnalysis.Execution> executions() throws IOException {
        List<E2eAnalysis.Execution> executions = new ArrayList<>();
        try (Stream<Path> files = Files.list(logDir)) {
            for (Path file : files.filter(p -> p.getFileName().toString().startsWith("executions-")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (String line : lines.subList(1, lines.size())) {
                    String[] f = line.split(",");
                    if (f.length == 9) {
                        executions.add(new E2eAnalysis.Execution(f[0], f[1], Long.parseLong(f[5]),
                                Long.parseLong(f[6]), f[7], f[8]));
                    }
                }
            }
        }
        return executions;
    }

    private static Map<String, Double> reasons(E2eSampler.Sample s) {
        Map<String, Double> result = new LinkedHashMap<>();
        s.values().forEach((key, value) -> {
            if (key.contains(".reason.") && !key.endsWith("DEPTH_SAMPLE") && !key.endsWith("EMPTY_POLL")) {
                result.merge(key.substring(key.indexOf(".reason.") + 8), value, Math::max);
            }
        });
        return result;
    }

    /** Value changes only, as "seconds:value". */
    private List<String> series(List<E2eSampler.Sample> samples, Function<E2eSampler.Sample, Double> value) {
        List<String> points = new ArrayList<>();
        Double previous = null;
        for (E2eSampler.Sample s : samples) {
            Double v = value.apply(s);
            if (!v.equals(previous)) {
                points.add((s.epochMillis() - goMillis) / 1000 + "s:" + v.intValue());
                previous = v;
            }
        }
        return points;
    }

    private void check(String name, boolean passed, Object detail) {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("name", name);
        check.put("passed", passed);
        check.put("detail", detail);
        checks.add(check);
    }

    private boolean writeReport(Path outDir) throws IOException {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("scenario", scenario);
        report.put("checks", checks);
        report.put("observations", observations);
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .writeValue(outDir.resolve("report.json").toFile(), report);
        boolean passed = checks.stream().allMatch(c -> Boolean.TRUE.equals(c.get("passed")));
        System.out.println("==== scenario " + scenario + " ====");
        for (Map<String, Object> c : checks) {
            System.out.printf("%-4s %-34s %s%n", Boolean.TRUE.equals(c.get("passed")) ? "PASS" : "FAIL", c.get("name"),
                    c.get("detail"));
        }
        if (!observations.isEmpty()) {
            System.out.println("observations " + observations);
        }
        System.out.println("Result " + scenario + ": " + (passed ? "PASS" : "FAIL"));
        return passed;
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
