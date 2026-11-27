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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.Workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Turns the run's raw data into pass/fail checks and a report. Correctness comes from the server's task records and
 * the workers' own execution logs; behavior comes from the one-second samples.
 */
class E2eAnalysis {

    private static final List<String> LOAD_PHASES = List.of("warmup", "steady", "burst", "slowdown");

    private final List<E2eDriver.StartedWorkflow> started;
    private final Map<String, Workflow> workflows;
    private final List<E2eSampler.Sample> samples;
    private final int workerCount;
    private final Path logDir;
    private final int startErrors;
    private final boolean adaptive = "adaptive".equals(E2eConfig.env("E2E_MODE", "adaptive"));
    private final int standbyMillis = E2eConfig.envInt("E2E_STANDBY_POLL_TIMEOUT_MS", 1000);
    private final List<Map<String, Object>> checks = new ArrayList<>();

    E2eAnalysis(List<E2eDriver.StartedWorkflow> started, Map<String, Workflow> workflows,
            List<E2eSampler.Sample> samples, int workerCount, Path logDir, int startErrors) {
        this.started = started;
        this.workflows = workflows;
        this.samples = samples;
        this.workerCount = workerCount;
        this.logDir = logDir;
        this.startErrors = startErrors;
    }

    /** One execution from a worker's log. */
    record Execution(String taskId, String taskType, long start, long end, String status, String worker) {}

    boolean writeReport(Path outDir) throws IOException {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("mode", adaptive ? "adaptive" : "fixed");
        List<Execution> executions = readExecutions();
        List<Task> tasks = workflows.values().stream()
                .flatMap(workflow -> workflow.getTasks().stream())
                .filter(task -> E2eConfig.TASK_TYPES.contains(task.getTaskDefName()))
                .toList();

        checkCompletion(report);
        checkExecutions(report, executions, tasks);
        checkConcurrency(report, executions);
        checkDomain(report, tasks);
        checkSlotsReleased(report);
        checkIdle(report);
        report.put("queueWaitMillis", queueWaits(tasks));
        report.put("phases", phaseSummary());
        report.put("burstDrainSeconds", burstDrainSeconds());
        report.put("adjustmentReasons", reasons());
        report.put("checks", checks);

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(outDir.resolve("report.json").toFile(), report);
        boolean passed = checks.stream().allMatch(check -> Boolean.TRUE.equals(check.get("passed")));
        System.out.println();
        System.out.println("==== Adaptive concurrency end-to-end: " + report.get("mode") + " ====");
        Map<String, Object> summary = new LinkedHashMap<>();
        for (String key : List.of("workflowStatus", "queueWaitMillis", "phases", "burstDrainSeconds")) {
            summary.put(key, report.get(key));
        }
        System.out.println(mapper.writeValueAsString(summary));
        for (Map<String, Object> check : checks) {
            System.out.printf("%-4s %-28s %s%n", Boolean.TRUE.equals(check.get("passed")) ? "PASS" : "FAIL",
                    check.get("name"), check.get("detail"));
        }
        System.out.println("Result: " + (passed ? "PASS" : "FAIL") + "  (report: " + outDir.resolve("report.json")
                + ")");
        return passed;
    }

    private void check(String name, boolean passed, Object detail) {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("name", name);
        check.put("passed", passed);
        check.put("detail", detail);
        checks.add(check);
    }

    private void checkCompletion(Map<String, Object> report) {
        Map<String, Long> byStatus = workflows.values().stream()
                .collect(Collectors.groupingBy(workflow -> workflow.getStatus().name(), TreeMap::new,
                        Collectors.counting()));
        Map<String, Long> byName = started.stream()
                .collect(Collectors.groupingBy(E2eDriver.StartedWorkflow::workflowName, TreeMap::new,
                        Collectors.counting()));
        report.put("workflowsStarted", byName);
        report.put("workflowStatus", byStatus);
        List<String> notCompleted = workflows.values().stream()
                .filter(workflow -> workflow.getStatus() != Workflow.WorkflowStatus.COMPLETED)
                .map(workflow -> workflow.getWorkflowId() + ":" + workflow.getStatus())
                .limit(10)
                .toList();
        long completed = byStatus.getOrDefault("COMPLETED", 0L);
        check("all workflows completed", completed == started.size() && startErrors == 0,
                completed + "/" + started.size() + " completed, " + startErrors + " start errors"
                        + (notCompleted.isEmpty() ? "" : ", e.g. " + notCompleted));
    }

    private void checkExecutions(Map<String, Object> report, List<Execution> executions, List<Task> tasks) {
        Map<String, Long> terminalByTask = executions.stream()
                .filter(execution -> !"IN_PROGRESS".equals(execution.status()))
                .collect(Collectors.groupingBy(Execution::taskId, Collectors.counting()));
        List<String> duplicates = terminalByTask.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        long missing = tasks.stream()
                .filter(task -> task.getStatus() == Task.Status.COMPLETED)
                .filter(task -> !terminalByTask.containsKey(task.getTaskId()))
                .count();
        long failures = executions.stream().filter(execution -> "EXCEPTION".equals(execution.status())).count();
        long retried = tasks.stream().filter(task -> task.getRetryCount() > 0).count();
        report.put("executions", Map.of("total", executions.size(), "injectedFailures", failures,
                "serverRetries", retried));
        check("no duplicate executions", duplicates.isEmpty(),
                duplicates.size() + " task ids executed to completion more than once"
                        + (duplicates.isEmpty() ? "" : ", e.g. " + duplicates.subList(0, Math.min(5,
                                duplicates.size()))));
        check("every task execution logged", missing == 0,
                missing + " completed server tasks have no worker execution record");
    }

    private void checkConcurrency(Map<String, Object> report, List<Execution> executions) {
        int maxProcess = E2eConfig.envInt("E2E_MAX_PROCESS_CONCURRENCY", 256);
        int maxWorker = E2eConfig.envInt("E2E_MAX_WORKER_CONCURRENCY", 64);
        Map<String, Integer> fixed = E2eConfig.envIntMap("E2E_FIXED_THREADS",
                "e2e_fetch_profile=8,e2e_score=3,e2e_persist=10,e2e_notify_partner=2");
        Map<String, Map<String, Integer>> peaks = new TreeMap<>();
        List<String> violations = new ArrayList<>();
        executions.stream().collect(Collectors.groupingBy(Execution::worker)).forEach((worker, rows) -> {
            Map<String, Integer> peak = new TreeMap<>();
            peak.put("total", peakConcurrency(rows));
            rows.stream().collect(Collectors.groupingBy(Execution::taskType))
                    .forEach((taskType, typed) -> peak.put(taskType, peakConcurrency(typed)));
            peaks.put(worker, peak);
            peak.forEach((key, value) -> {
                int limit = adaptive ? ("total".equals(key) ? maxProcess : maxWorker)
                        : ("total".equals(key) ? Integer.MAX_VALUE : fixed.getOrDefault(key, 1));
                if (value > limit) {
                    violations.add(worker + "/" + key + "=" + value + ">" + limit);
                }
            });
        });
        report.put("peakConcurrency", peaks);
        check("concurrency within ceilings", violations.isEmpty() && !peaks.isEmpty(),
                violations.isEmpty() ? "peaks " + peaks : violations);
    }

    private static int peakConcurrency(List<Execution> rows) {
        List<long[]> events = new ArrayList<>();
        for (Execution row : rows) {
            events.add(new long[] {row.start(), 1});
            events.add(new long[] {row.end(), -1});
        }
        // Ends sort before starts at the same millisecond: a slot freed and reused is not an overlap.
        events.sort((a, b) -> a[0] != b[0] ? Long.compare(a[0], b[0]) : Long.compare(a[1], b[1]));
        int current = 0;
        int peak = 0;
        for (long[] event : events) {
            current += (int) event[1];
            peak = Math.max(peak, current);
        }
        return peak;
    }

    private void checkDomain(Map<String, Object> report, List<Task> tasks) {
        List<Task> notify = tasks.stream()
                .filter(task -> E2eConfig.NOTIFY_PARTNER.equals(task.getTaskDefName()))
                .toList();
        long outsideDomain = notify.stream()
                .filter(task -> !E2eConfig.PARTNER_DOMAIN.equals(task.getDomain()))
                .count();
        List<String> probeIds = started.stream().filter(E2eDriver.StartedWorkflow::probe)
                .map(E2eDriver.StartedWorkflow::workflowId).toList();
        long probesRouted = probeIds.stream()
                .map(workflows::get)
                .filter(workflow -> workflow != null && workflow.getStatus() == Workflow.WorkflowStatus.COMPLETED)
                .flatMap(workflow -> workflow.getTasks().stream())
                .filter(task -> E2eConfig.NOTIFY_PARTNER.equals(task.getTaskDefName()))
                .filter(task -> E2eConfig.PARTNER_DOMAIN.equals(task.getDomain()))
                .map(Task::getWorkflowInstanceId)
                .distinct()
                .count();
        check("domain routing kept alive", outsideDomain == 0 && probesRouted == probeIds.size(),
                notify.size() + " partner tasks, " + outsideDomain + " fell back to the default queue; "
                        + probesRouted + "/" + probeIds.size() + " post-idle probes routed to the domain");
    }

    private void checkSlotsReleased(Map<String, Object> report) {
        E2eSampler.Sample last = samples.get(samples.size() - 1);
        List<String> leaks = new ArrayList<>();
        // Each idle queue may hold one slot for its standby poll.
        int standbySlots = adaptive && standbyMillis > 0 ? E2eConfig.TASK_TYPES.size() : 0;
        for (int i = 0; i < workerCount; i++) {
            double inflight = last.get("w" + i + ".inflight");
            double held = adaptive ? last.get("w" + i + ".processHeld") : 0;
            if (inflight != 0 || held > standbySlots) {
                leaks.add("w" + i + " inflight=" + inflight + " held=" + held);
            }
        }
        check("slots released after drain", leaks.isEmpty(), leaks.isEmpty()
                ? "no executions, at most " + standbySlots + " slots held by standby polls" : leaks);
    }

    private void checkIdle(Map<String, Object> report) {
        List<E2eSampler.Sample> idle = phase("idle");
        if (idle.size() < 40) {
            check("idle polling backs off", false, "idle phase too short: " + idle.size() + "s");
            return;
        }
        // Skip the first 30 seconds: the backlog drains and backoff ramps up.
        E2eSampler.Sample first = idle.get(30);
        E2eSampler.Sample last = idle.get(idle.size() - 1);
        double seconds = (last.epochMillis() - first.epochMillis()) / 1000.0;
        Map<String, Double> pollRates = new TreeMap<>();
        double maxRate = 0;
        for (int i = 0; i < workerCount; i++) {
            for (String taskType : E2eConfig.TASK_TYPES) {
                String key = "w" + i + "." + taskType + ".polls";
                double rate = (last.get(key) - first.get(key)) / seconds;
                pollRates.put(key, round(rate));
                maxRate = Math.max(maxRate, rate);
            }
        }
        report.put("idlePollsPerSecond", pollRates);
        if (adaptive) {
            // One standby poll per timeout, or one heartbeat per interval without standby polls.
            double limit = standbyMillis > 0 ? 1.1 * 1000 / standbyMillis : 0.5;
            check("idle polling backs off", maxRate <= limit,
                    "max " + round(maxRate) + " polls/s per queue per process (limit " + round(limit) + ")");
            double peak = samples.stream().mapToDouble(sample -> maxOverWorkers(sample, "processTarget"))
                    .filter(value -> !Double.isNaN(value)).max().orElse(Double.NaN);
            double end = maxOverWorkers(last, "processTarget");
            // Decay never goes below the configured minimum.
            int minimum = E2eConfig.envInt("E2E_MIN_PROCESS_CONCURRENCY", 1);
            check("process target decays when idle", end <= Math.max(minimum, Math.ceil(peak / 2)),
                    "peak " + peak + ", end of idle " + end + " (minimum " + minimum + ")");
        } else {
            check("idle polling backs off", true, "fixed mode reference: max " + round(maxRate) + " polls/s");
        }
    }

    private Map<String, Map<String, Object>> queueWaits(List<Task> tasks) {
        Map<String, String> phaseByWorkflow = started.stream()
                .collect(Collectors.toMap(E2eDriver.StartedWorkflow::workflowId, E2eDriver.StartedWorkflow::phase));
        Map<String, Map<String, Object>> result = new TreeMap<>();
        tasks.stream()
                .filter(task -> task.getRetryCount() == 0 && task.getStartTime() > 0)
                .collect(Collectors.groupingBy(task -> phaseByWorkflow.getOrDefault(task.getWorkflowInstanceId(),
                        "?") + "/" + task.getTaskDefName()))
                .forEach((key, group) -> {
                    List<Long> waits = group.stream().map(task -> task.getStartTime() - task.getScheduledTime())
                            .sorted().toList();
                    Map<String, Object> stats = new LinkedHashMap<>();
                    stats.put("n", waits.size());
                    stats.put("p50", percentile(waits, 0.50));
                    stats.put("p99", percentile(waits, 0.99));
                    stats.put("max", waits.get(waits.size() - 1));
                    result.put(key, stats);
                });
        return result;
    }

    private Map<String, Map<String, Object>> phaseSummary() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (String name : Stream.concat(LOAD_PHASES.stream(), Stream.of("idle")).toList()) {
            List<E2eSampler.Sample> rows = phase(name);
            if (rows.isEmpty()) {
                continue;
            }
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("seconds", rows.size());
            summary.put("profileMaxInFlight", max(rows, sample -> sample.get("sim.profile.maxInFlight")));
            summary.put("profileP99Millis", max(rows, sample -> sample.get("sim.profile.p99Millis")));
            summary.put("persistMaxInFlight", max(rows, sample -> sample.get("sim.persist.maxInFlight")));
            for (String queue : List.of(E2eConfig.FETCH_PROFILE, E2eConfig.SCORE, E2eConfig.PERSIST)) {
                summary.put("maxDepth." + queue, max(rows, sample -> sample.get("depth." + queue)));
            }
            for (int i = 0; i < workerCount; i++) {
                String prefix = "w" + i + ".";
                summary.put(prefix + "cpuAvg", round(rows.stream().mapToDouble(s -> s.get(prefix + "cpu"))
                        .filter(v -> !Double.isNaN(v)).average().orElse(Double.NaN)));
                summary.put(prefix + "cpuMax", max(rows, sample -> sample.get(prefix + "cpu")));
                summary.put(prefix + "heapMaxMb", max(rows, sample -> sample.get(prefix + "heapUsedMb")));
                summary.put(prefix + "inflightMax", max(rows, sample -> sample.get(prefix + "inflight")));
                if (adaptive) {
                    summary.put(prefix + "processTargetMax", max(rows, s -> s.get(prefix + "processTarget")));
                    summary.put(prefix + "pausedSeconds", rows.stream().filter(s -> s.get(prefix + "paused") == 1)
                            .count());
                    for (String taskType : E2eConfig.TASK_TYPES) {
                        summary.put(prefix + taskType + ".targetMax",
                                max(rows, s -> s.get(prefix + taskType + ".target")));
                    }
                }
                E2eSampler.Sample first = rows.get(0);
                E2eSampler.Sample last = rows.get(rows.size() - 1);
                double seconds = Math.max(1, (last.epochMillis() - first.epochMillis()) / 1000.0);
                for (String key : List.of("req.queueSizes", "req.poll", "req.update")) {
                    summary.put(prefix + key + "PerSec",
                            round((last.get(prefix + key) - first.get(prefix + key)) / seconds));
                }
            }
            result.put(name, summary);
        }
        return result;
    }

    /** Seconds from the burst until the fetch and persist backlog first falls back below 20. */
    private Double burstDrainSeconds() {
        List<E2eSampler.Sample> after = samples.stream()
                .filter(sample -> List.of("burst", "slowdown", "idle", "probe", "drain").contains(sample.phase()))
                .toList();
        if (after.isEmpty()) {
            return null;
        }
        boolean backlogged = false;
        for (E2eSampler.Sample sample : after) {
            double backlog = sample.get("depth." + E2eConfig.FETCH_PROFILE) + sample.get("depth." + E2eConfig.PERSIST);
            backlogged |= backlog >= 100;
            if (backlogged && backlog < 20) {
                return round((sample.epochMillis() - after.get(0).epochMillis()) / 1000.0);
            }
        }
        return null;
    }

    private Map<String, Double> reasons() {
        E2eSampler.Sample last = samples.get(samples.size() - 1);
        Map<String, Double> result = new TreeMap<>();
        last.values().forEach((key, value) -> {
            if (key.contains(".reason.")) {
                result.put(key, value);
            }
        });
        return result;
    }

    private List<E2eSampler.Sample> phase(String name) {
        return samples.stream().filter(sample -> name.equals(sample.phase())).toList();
    }

    private double maxOverWorkers(E2eSampler.Sample sample, String key) {
        double max = Double.NaN;
        for (int i = 0; i < workerCount; i++) {
            double value = sample.get("w" + i + "." + key);
            if (!Double.isNaN(value)) {
                max = Double.isNaN(max) ? value : Math.max(max, value);
            }
        }
        return max;
    }

    private static double max(List<E2eSampler.Sample> rows, ToDoubleFunction<E2eSampler.Sample> value) {
        return round(rows.stream().mapToDouble(value).filter(v -> !Double.isNaN(v)).max().orElse(Double.NaN));
    }

    private static long percentile(List<Long> sorted, double quantile) {
        return sorted.get(Math.min(sorted.size() - 1, (int) Math.ceil(quantile * sorted.size()) - 1));
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private List<Execution> readExecutions() throws IOException {
        List<Execution> executions = new ArrayList<>();
        if (!Files.isDirectory(logDir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> files = Files.list(logDir)) {
            for (Path file : files.filter(path -> path.getFileName().toString().startsWith("executions-")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (String line : lines.subList(1, lines.size())) {
                    String[] f = line.split(",");
                    if (f.length == 9) {
                        executions.add(new Execution(f[0], f[1], Long.parseLong(f[5]), Long.parseLong(f[6]), f[7],
                                f[8]));
                    }
                }
            }
        }
        return executions;
    }
}
