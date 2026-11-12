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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Checks a bench run. Inputs are the files the bench wrote: the client history, the worker ledgers,
 * the final state of every execution, and the worker logs. Each check either finds a violation of
 * a guarantee native workflows make ({@link Level#ERROR}), or measures behaviour the guarantees
 * allow but an operator should know about ({@link Level#INFO}).
 *
 * <p>Run on its own to re-check an old run: {@code java durability.Checker build/durability/<run>}.
 */
public final class Checker {

    public enum Level { ERROR, INFO }

    public record Finding(Level level, String check, String key, String detail) {}

    public record Report(List<Finding> findings, Map<String, Object> stats, Map<String, Integer> counts) {

        public boolean valid() {
            return findings.stream().noneMatch(f -> f.level() == Level.ERROR);
        }

        public String render() {
            StringBuilder b = new StringBuilder();
            b.append(valid() ? "VALID" : "INVALID").append(": native workflow durability check\n\n");
            b.append(String.format("%-34s %-6s %s%n", "check", "level", "count"));
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                b.append(String.format("%-34s %-6s %d%n", e.getKey(), level(e.getKey()), e.getValue()));
            }
            b.append("\nstats\n");
            stats.forEach((k, v) -> b.append(String.format("  %-32s %s%n", k, v)));
            List<Finding> errors = findings.stream().filter(f -> f.level() == Level.ERROR).toList();
            if (!errors.isEmpty()) {
                b.append("\nerrors (first 40)\n");
                errors.stream().limit(40).forEach(f ->
                        b.append("  ").append(f.check()).append(" ").append(f.key()).append(": ").append(f.detail()).append('\n'));
            }
            return b.toString();
        }

        private String level(String check) {
            return CHECKS.getOrDefault(check, Level.INFO).name();
        }
    }

    /** Every check, in report order, with its level. Zero counts are reported too. */
    static final Map<String, Level> CHECKS = new LinkedHashMap<>();

    static {
        // executions
        CHECKS.put("lost-execution", Level.ERROR);           // acknowledged start, execution gone
        CHECKS.put("duplicate-execution", Level.ERROR);      // more than one execution for one idempotency key
        CHECKS.put("idempotency-mismatch", Level.ERROR);     // two acknowledged starts of one key returned different ids
        CHECKS.put("null-workflow-id", Level.ERROR);         // start acknowledged with no id
        CHECKS.put("never-acknowledged", Level.ERROR);       // a key never got an acknowledged start, even after recovery
        CHECKS.put("incomplete", Level.ERROR);               // not COMPLETED after recovery
        CHECKS.put("wrong-output", Level.ERROR);             // total or id differs from the expected result
        CHECKS.put("nondeterminism", Level.ERROR);           // replay divergence reported by the SDK
        // recorded values
        CHECKS.put("replay-divergence", Level.ERROR);        // once/random/now changed after being recorded, or differ from the output
        // signals and updates
        CHECKS.put("lost-signal", Level.ERROR);              // acknowledged deposit missing from the output
        CHECKS.put("duplicate-signal", Level.ERROR);         // a deposit applied twice
        CHECKS.put("phantom-signal", Level.ERROR);           // a deposit nobody sent
        CHECKS.put("signal-reordered", Level.ERROR);         // one client's acknowledged deposits applied out of order
        CHECKS.put("lost-update", Level.ERROR);              // acknowledged updates that returned the same value, or fewer applied than acknowledged
        CHECKS.put("phantom-update", Level.ERROR);           // more updates applied than were attempted
        CHECKS.put("update-order", Level.ERROR);             // an update acknowledged before another was sent returned a larger value
        // queries
        CHECKS.put("query-not-prefix", Level.ERROR);         // an answer that is not a prefix of the final deposits
        CHECKS.put("query-went-back", Level.ERROR);          // one client's later answer is shorter than its earlier one
        CHECKS.put("query-final-mismatch", Level.ERROR);     // a query after the execution ended differs from its output
        CHECKS.put("query-mutated", Level.ERROR);            // a query's write reached the execution
        // tasks
        CHECKS.put("task-never-executed", Level.ERROR);      // completed execution whose step has no successful attempt in any ledger
        CHECKS.put("task-output-mismatch", Level.ERROR);     // a server step output that differs from the value its worker returned
        // allowed by the guarantees, measured
        CHECKS.put("indefinite-start-took-effect", Level.INFO);
        CHECKS.put("lost-at-completion", Level.ERROR);       // acknowledged within 100 ms of completion and lost: the push/completion race
        CHECKS.put("indefinite-signal-applied", Level.INFO);
        CHECKS.put("indefinite-signal-dropped", Level.INFO);
        CHECKS.put("duplicate-task-execution", Level.INFO);  // at-least-once: a step succeeded more than once
        CHECKS.put("concurrent-task-execution", Level.INFO); // two attempts of one step overlapped in time
        CHECKS.put("continuation-retried", Level.INFO);      // a continuation's lease was handed out more than once, or it was deferred (failover)
        CHECKS.put("continuation-stalled", Level.INFO);      // a continuation took over 20 s from queued to applied
        CHECKS.put("query-lag", Level.INFO);                 // an answer missed deposits acknowledged before the query was sent
        CHECKS.put("unrecorded-value-discarded", Level.INFO); // an attempt that never completed saw other once/random/now values
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final List<Finding> findings = new ArrayList<>();
    private final Map<String, Object> stats = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {
        Report report = check(Path.of(args[0]));
        System.out.println(report.render());
        System.exit(report.valid() ? 0 : 1);
    }

    public static Report check(Path dir) throws IOException {
        Checker c = new Checker();
        List<Map<String, Object>> history = readLines(dir.resolve("history.jsonl"));
        List<Map<String, Object>> ledger = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir.resolve("ledger"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".jsonl")).toList()) {
                ledger.addAll(readLines(f));
            }
        }
        Map<String, Object> fin = JSON.readValue(dir.resolve("final.json").toFile(), new TypeReference<>() {});
        List<String> workerLogs = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.getFileName().toString().startsWith("worker-")).toList()) {
                workerLogs.add(Files.readString(f));
            }
        }
        c.run(history, ledger, fin, workerLogs);
        Map<String, Integer> counts = new LinkedHashMap<>();
        CHECKS.keySet().forEach(k -> counts.put(k, 0));
        c.findings.forEach(f -> counts.merge(f.check(), 1, Integer::sum));
        return new Report(c.findings, c.stats, counts);
    }

    @SuppressWarnings("unchecked")
    void run(List<Map<String, Object>> history, List<Map<String, Object>> ledger, Map<String, Object> fin,
             List<String> workerLogs) {
        Map<String, List<Map<String, Object>>> byKey = (Map<String, List<Map<String, Object>>>) fin.get("byKey");
        Map<String, Map<String, Object>> byId = (Map<String, Map<String, Object>>) fin.get("byId");
        Map<String, Object> doneAt = (Map<String, Object>) fin.getOrDefault("doneAt", Map.of());

        checkStarts(history, byKey, byId, doneAt);
        Map<String, Map<String, Object>> executions = new LinkedHashMap<>(); // key -> the execution
        byKey.forEach((key, list) -> {
            if (!list.isEmpty()) {
                executions.put(key, list.get(0));
            }
        });
        checkOutputs(executions);
        Map<String, Integer> handled = new HashMap<>();
        for (Map<String, Object> line : ledger) {
            if ("handled".equals(line.get("kind"))) {
                handled.merge((String) line.get("key"), 1, Integer::sum);
            }
        }
        checkSignals(history, executions, handled);
        checkUpdates(history, executions);
        checkUpdateOrder(history);
        checkQueries(history, executions);
        checkLedger(ledger, executions);
        checkNondeterminism(executions, workerLogs);
        stats(history, ledger, executions);
    }

    // ---- executions ----

    private void checkStarts(List<Map<String, Object>> history, Map<String, List<Map<String, Object>>> byKey,
                             Map<String, Map<String, Object>> byId, Map<String, Object> doneAt) {
        Map<String, Set<String>> acknowledged = new HashMap<>();
        Set<String> indefinite = new HashSet<>();
        for (Map<String, Object> op : ops(history, "start")) {
            String key = (String) op.get("key");
            switch ((String) op.get("type")) {
                case "ok" -> {
                    Object id = op.get("value");
                    if (id == null || id.toString().isBlank()) {
                        error("null-workflow-id", key, "start returned no id");
                    } else {
                        acknowledged.computeIfAbsent(key, k -> new HashSet<>()).add(id.toString());
                    }
                }
                case "info", "fail" -> indefinite.add(key);
                default -> {}
            }
        }
        for (Map.Entry<String, Set<String>> e : acknowledged.entrySet()) {
            if (e.getValue().size() > 1) {
                error("idempotency-mismatch", e.getKey(), "acknowledged ids " + e.getValue());
            }
            for (String id : e.getValue()) {
                if (byId.get(id) == null) {
                    error("lost-execution", e.getKey(), "execution " + id + " was acknowledged but is not found");
                }
            }
        }
        for (Map.Entry<String, List<Map<String, Object>>> e : byKey.entrySet()) {
            String key = e.getKey();
            List<Map<String, Object>> list = e.getValue();
            if (list.size() > 1) {
                error("duplicate-execution", key, list.size() + " executions: "
                        + list.stream().map(w -> w.get("workflowId") + "=" + w.get("status")).toList());
            }
            if (!acknowledged.containsKey(key)) {
                error("never-acknowledged", key, list.isEmpty() ? "no execution exists"
                        : "execution " + list.get(0).get("workflowId") + " exists but no start was acknowledged");
            }
            if (indefinite.contains(key) && !list.isEmpty()) {
                info("indefinite-start-took-effect", key, "a failed or timed-out start attempt; the execution exists");
            }
            if (!list.isEmpty() && !"COMPLETED".equals(list.get(0).get("status"))) {
                error("incomplete", key, list.get(0).get("workflowId") + " is " + list.get(0).get("status")
                        + (list.get(0).get("reason") == null ? "" : ": " + list.get(0).get("reason"))
                        + (doneAt.containsKey(key) ? "" : " (did not converge before the recovery deadline)"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void checkOutputs(Map<String, Map<String, Object>> executions) {
        executions.forEach((key, wf) -> {
            if (!"COMPLETED".equals(wf.get("status"))) {
                return;
            }
            Map<String, Object> output = (Map<String, Object>) wf.get("output");
            int expected = LedgerModel.expectedTotal(key);
            Object total = output == null ? null : output.get("total");
            if (!key.equals(output == null ? null : output.get("id")) || !(total instanceof Number n) || n.intValue() != expected) {
                error("wrong-output", key, "expected id " + key + " and total " + expected + ", got " + output);
            }
            for (Map<String, Object> t : (List<Map<String, Object>>) wf.get("tasks")) {
                if ("durability_step".equals(t.get("type")) && "COMPLETED".equals(t.get("status"))) {
                    Map<String, Object> out = (Map<String, Object>) t.get("output");
                    Object result = out == null ? null : out.get("result");
                    if (result instanceof Number r && !matchesSomeStep(key, r.intValue())) {
                        error("task-output-mismatch", key, t.get("ref") + " output " + out);
                    }
                }
            }
        });
    }

    private static boolean matchesSomeStep(String key, int value) {
        for (int step = 1; step <= LedgerWorkflow.STEPS; step++) {
            if (LedgerModel.expectedStep(key, step) == value) {
                return true;
            }
        }
        return false;
    }

    // ---- signals and updates ----

    @SuppressWarnings("unchecked")
    private void checkSignals(List<Map<String, Object>> history, Map<String, Map<String, Object>> executions,
                              Map<String, Integer> handled) {
        Map<String, List<String>> acknowledged = new HashMap<>(); // key -> deposit ids in acknowledgement order
        Map<String, String> sender = new HashMap<>();             // deposit id -> the process that sent it
        Map<String, Set<String>> attempted = new HashMap<>();
        Map<String, Set<String>> indefinite = new HashMap<>();
        Map<String, String> invokedValue = new HashMap<>();
        Map<String, Long> ackedAt = new HashMap<>();
        for (Map<String, Object> op : ops(history, "deposit")) {
            String key = (String) op.get("key");
            String type = (String) op.get("type");
            if (type.equals("invoke")) {
                attempted.computeIfAbsent(key, k -> new HashSet<>()).add((String) op.get("value"));
                invokedValue.put(op.get("process") + "", (String) op.get("value"));
            } else if (type.equals("ok")) {
                acknowledged.computeIfAbsent(key, k -> new ArrayList<>()).add((String) op.get("value"));
                ackedAt.put((String) op.get("value"), ((Number) op.get("t")).longValue());
                sender.put((String) op.get("value"), String.valueOf(op.get("process")));
            } else if (type.equals("info")) {
                indefinite.computeIfAbsent(key, k -> new HashSet<>()).add(invokedValue.get(op.get("process") + ""));
            }
        }
        executions.forEach((key, wf) -> {
            if (!"COMPLETED".equals(wf.get("status"))) {
                return;
            }
            Map<String, Object> output = (Map<String, Object>) wf.get("output");
            List<String> applied = output == null || output.get("deposits") == null
                    ? List.of() : (List<String>) output.get("deposits");
            Set<String> seen = new HashSet<>();
            for (String d : applied) {
                if (!seen.add(d)) {
                    error("duplicate-signal", key, d + " applied more than once");
                }
                if (!attempted.getOrDefault(key, Set.of()).contains(d)) {
                    error("phantom-signal", key, d + " was never sent to this execution");
                }
            }
            List<String> acked = acknowledged.getOrDefault(key, List.of());
            for (String d : acked) {
                long endTime = wf.get("endTime") instanceof Number e ? e.longValue() : Long.MAX_VALUE;
                // a signal acknowledged around the moment of completion and lost: the race
                // between a push and a completing decide, closed by pushing under the
                // workflow's execution lock; reported separately to point at that path
                if (!seen.contains(d) && endTime - ackedAt.getOrDefault(d, 0L) < 100) {
                    error("lost-at-completion", key, d + " was acknowledged " + (endTime - ackedAt.getOrDefault(d, 0L))
                            + " ms before the execution completed and is not in its output");
                } else if (!seen.contains(d)) {
                    int times = handled.getOrDefault(d, 0);
                    error("lost-signal", key, d + " was acknowledged but is not in the output; "
                            + (times == 0 ? "no handler ever applied it (lost before the handler lane)"
                                    : "a handler applied it " + times + " time(s) (its write was overwritten)"));
                }
            }
            // each client sends sequentially, so its acknowledged deposits must apply in send order
            for (String process : new HashSet<>(sender.values())) {
                List<String> mine = acked.stream().filter(d -> process.equals(sender.get(d))).toList();
                List<String> appliedMine = applied.stream().filter(new HashSet<>(mine)::contains).distinct().toList();
                if (!appliedMine.equals(mine.stream().filter(seen::contains).toList())) {
                    error("signal-reordered", key, process + " acknowledged " + mine + " applied as " + appliedMine);
                }
            }
            for (String d : indefinite.getOrDefault(key, Set.of())) {
                if (d != null) {
                    if (seen.contains(d)) {
                        info("indefinite-signal-applied", key, d);
                    } else {
                        info("indefinite-signal-dropped", key, d);
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void checkUpdates(List<Map<String, Object>> history, Map<String, Map<String, Object>> executions) {
        Map<String, List<Integer>> returned = new HashMap<>();
        Map<String, Integer> attempted = new HashMap<>();
        for (Map<String, Object> op : ops(history, "bump")) {
            String key = (String) op.get("key");
            if ("invoke".equals(op.get("type"))) {
                attempted.merge(key, 1, Integer::sum);
            } else if ("ok".equals(op.get("type"))) {
                Map<String, Object> v = (Map<String, Object>) op.get("value");
                returned.computeIfAbsent(key, k -> new ArrayList<>()).add(((Number) v.get("value")).intValue());
            }
        }
        executions.forEach((key, wf) -> {
            List<Integer> values = returned.getOrDefault(key, List.of());
            Set<Integer> distinct = new HashSet<>(values);
            if (distinct.size() != values.size()) {
                error("lost-update", key, "acknowledged updates returned repeated counter values " + values);
            }
            if (!"COMPLETED".equals(wf.get("status"))) {
                return;
            }
            Map<String, Object> output = (Map<String, Object>) wf.get("output");
            int bumps = output == null || !(output.get("bumps") instanceof Number n) ? 0 : n.intValue();
            if (bumps < values.size()) {
                error("lost-update", key, values.size() + " updates acknowledged but the execution applied " + bumps);
            }
            int max = values.stream().mapToInt(Integer::intValue).max().orElse(0);
            if (bumps < max) {
                error("lost-update", key, "an update returned " + max + " but the final count is " + bumps);
            }
            if (bumps > attempted.getOrDefault(key, 0)) {
                error("phantom-update", key, bumps + " updates applied but " + attempted.getOrDefault(key, 0) + " attempted");
            }
        });
    }

    /** An operation from invoke to completion, for one process (which runs one at a time). */
    private record Op(String process, String key, long invokedAt, long completedAt, String type, Object value) {}

    private static List<Op> completed(List<Map<String, Object>> history, String f) {
        Map<String, Map<String, Object>> open = new HashMap<>();
        List<Op> ops = new ArrayList<>();
        for (Map<String, Object> line : ops(history, f)) {
            String process = String.valueOf(line.get("process"));
            if ("invoke".equals(line.get("type"))) {
                open.put(process, line);
            } else {
                Map<String, Object> inv = open.remove(process);
                if (inv != null) {
                    ops.add(new Op(process, (String) line.get("key"), ((Number) inv.get("t")).longValue(),
                            ((Number) line.get("t")).longValue(), (String) line.get("type"), line.get("value")));
                }
            }
        }
        return ops;
    }

    /**
     * Updates on one execution behave like one counter: if update A was acknowledged before
     * update B was sent, A applied first, so A returned the smaller value.
     */
    @SuppressWarnings("unchecked")
    private void checkUpdateOrder(List<Map<String, Object>> history) {
        Map<String, List<Op>> byKey = new HashMap<>();
        for (Op op : completed(history, "bump")) {
            if ("ok".equals(op.type())) {
                byKey.computeIfAbsent(op.key(), k -> new ArrayList<>()).add(op);
            }
        }
        byKey.forEach((key, ops) -> {
            for (Op a : ops) {
                for (Op b : ops) {
                    int va = ((Number) ((Map<String, Object>) a.value()).get("value")).intValue();
                    int vb = ((Number) ((Map<String, Object>) b.value()).get("value")).intValue();
                    if (a.completedAt() < b.invokedAt() && va >= vb) {
                        error("update-order", key, "update returning " + va + " was acknowledged before one that returned " + vb + " was sent");
                        return;
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void checkQueries(List<Map<String, Object>> history, Map<String, Map<String, Object>> executions) {
        Map<String, List<String>> finalDeposits = new HashMap<>();
        executions.forEach((key, wf) -> {
            if ("COMPLETED".equals(wf.get("status")) && wf.get("output") instanceof Map<?, ?> out
                    && out.get("deposits") instanceof List<?> d) {
                finalDeposits.put(key, (List<String>) d);
                if (d.contains(LedgerWorkflow.QUERY_MARKER)) {
                    error("query-mutated", key, "the deposits contain what a query wrote");
                }
            }
        });
        Map<String, Long> ackedAt = new HashMap<>(); // deposit id -> when it was acknowledged
        Map<String, String> depositKey = new HashMap<>();
        for (Map<String, Object> line : ops(history, "deposit")) {
            if ("ok".equals(line.get("type"))) {
                ackedAt.put((String) line.get("value"), ((Number) line.get("t")).longValue());
                depositKey.put((String) line.get("value"), (String) line.get("key"));
            }
        }
        Map<String, List<String>> lastAnswer = new HashMap<>(); // process|key -> previous answer
        int answers = 0;
        int lagging = 0;
        for (Op op : completed(history, "query")) {
            if (!"ok".equals(op.type())) {
                continue;
            }
            answers++;
            List<String> answer = op.value() instanceof List<?> l ? (List<String>) l : List.of();
            List<String> full = finalDeposits.get(op.key());
            if (full != null && (answer.size() > full.size() || !full.subList(0, answer.size()).equals(answer))) {
                error("query-not-prefix", op.key(), "answer " + answer + " is not a prefix of " + full);
            }
            if ("final-querier".equals(op.process()) && full != null && !answer.equals(full)) {
                error("query-final-mismatch", op.key(), "answered " + answer.size() + " deposits, output has " + full.size());
            }
            String slot = op.process() + "|" + op.key();
            List<String> previous = lastAnswer.put(slot, answer);
            if (previous != null && answer.size() < previous.size()) {
                error("query-went-back", op.key(), op.process() + " saw " + previous.size() + " deposits, then " + answer.size());
            }
            long missing = ackedAt.entrySet().stream()
                    .filter(e -> op.key().equals(depositKey.get(e.getKey())) && e.getValue() < op.invokedAt()
                            && !answer.contains(e.getKey()))
                    .count();
            if (missing > 0) {
                lagging++;
                info("query-lag", op.key(), missing + " acknowledged deposits not yet applied when " + op.process() + " queried");
            }
        }
        stats.put("queries answered", answers);
        stats.put("queries missing an acknowledged deposit", lagging);
    }

    // ---- worker ledgers ----

    @SuppressWarnings("unchecked")
    private void checkLedger(List<Map<String, Object>> ledger, Map<String, Map<String, Object>> executions) {
        Map<String, Map<String, Object>> byId = new HashMap<>();
        executions.values().forEach(wf -> byId.put((String) wf.get("workflowId"), wf));

        Map<String, Set<String>> observed = new TreeMap<>(); // workflowId:name -> distinct values
        Map<String, List<Object[]>> observedAt = new HashMap<>(); // workflowId:name -> [time, value]
        Map<String, Integer> successes = new HashMap<>();     // step key -> successful attempts
        Map<String, Map<String, long[]>> attempts = new HashMap<>(); // step key -> nonce -> [start, end]
        for (Map<String, Object> line : ledger) {
            String kind = (String) line.get("kind");
            String key = (String) line.get("key");
            long t = ((Number) line.get("t")).longValue();
            switch (kind) {
                case "observe" -> {
                    observed.computeIfAbsent(key, k -> new HashSet<>()).add(String.valueOf(line.get("value")));
                    observedAt.computeIfAbsent(key, k -> new ArrayList<>()).add(new Object[] {t, String.valueOf(line.get("value"))});
                }
                case "task-start" -> attempts.computeIfAbsent(key, k -> new HashMap<>())
                        .computeIfAbsent((String) line.get("nonce"), n -> new long[] {t, Long.MAX_VALUE})[0] = t;
                case "task-end", "task-fail" -> {
                    attempts.computeIfAbsent(key, k -> new HashMap<>())
                            .computeIfAbsent((String) line.get("nonce"), n -> new long[] {t, t})[1] = t;
                    if (kind.equals("task-end")) {
                        successes.merge(key, 1, Integer::sum);
                    }
                }
                default -> {}
            }
        }

        // Recorded values: once a value is recorded (the continuation that computed it was applied,
        // and its marker carries it), every replay must see it. An attempt whose completion was
        // never applied (killed, timed out, or set aside by a deferral) may have seen other values;
        // they were never recorded, so that is allowed.
        Map<String, Long> recordedAt = new HashMap<>(); // workflowId:name -> when the marker was applied
        Map<String, Object> recordedValue = new HashMap<>(); // workflowId:name -> the output's value
        for (Map<String, Object> wf : executions.values()) {
            if (wf.get("output") instanceof Map<?, ?> out) {
                for (String name : List.of("token", "random", "startedAt")) {
                    recordedValue.put(wf.get("workflowId") + ":" + name, out.get(name));
                }
            }
            for (Map<String, Object> marker : markers(wf)) {
                if (!"APPLIED".equals(marker.get("state")) || !(marker.get("appliedAt") instanceof Number at)
                        || !(marker.get("recorded") instanceof Map<?, ?> recorded)) {
                    continue;
                }
                for (Object ref : recorded.keySet()) {
                    String name = "now".equals(ref) ? "startedAt" : String.valueOf(ref);
                    recordedAt.put(wf.get("workflowId") + ":" + name, at.longValue());
                }
            }
        }
        observed.forEach((k, values) -> {
            if (values.size() <= 1) {
                return;
            }
            Long at = recordedAt.get(k);
            if (at == null) {
                error("replay-divergence", k, "replays observed " + values);
                return;
            }
            Set<String> after = new HashSet<>();
            Set<String> before = new HashSet<>();
            for (Object[] o : observedAt.get(k)) {
                ((Long) o[0] > at ? after : before).add((String) o[1]);
            }
            Object expected = recordedValue.get(k);
            if (after.size() > 1) {
                error("replay-divergence", k, "after it was recorded, replays observed " + after);
            } else if (after.size() == 1 && expected != null
                    && !numericEquals(after.iterator().next(), String.valueOf(expected))) {
                error("replay-divergence", k, "after it was recorded, replays observed " + after + " but the output has " + expected);
            } else {
                info("unrecorded-value-discarded", k, "attempts before recording saw " + before + ", recorded " + after);
            }
        });
        executions.forEach((key, wf) -> {
            String id = (String) wf.get("workflowId");
            Map<String, Object> output = (Map<String, Object>) wf.get("output");
            if (!"COMPLETED".equals(wf.get("status")) || output == null) {
                return;
            }
            compareObservation(observed, id, "token", output.get("token"), key);
            compareObservation(observed, id, "random", output.get("random"), key);
            compareObservation(observed, id, "startedAt", output.get("startedAt"), key);
            for (int step = 1; step <= LedgerWorkflow.STEPS; step++) {
                if (successes.getOrDefault(key + ":" + step, 0) == 0) {
                    error("task-never-executed", key, "step " + step + " completed on the server with no successful attempt in any ledger");
                }
            }
        });

        int duplicateSteps = 0;
        for (Map.Entry<String, Integer> e : successes.entrySet()) {
            if (e.getValue() > 1) {
                duplicateSteps++;
                info("duplicate-task-execution", e.getKey(), e.getValue() + " successful attempts");
            }
        }
        for (Map.Entry<String, Map<String, long[]>> e : attempts.entrySet()) {
            List<long[]> spans = new ArrayList<>(e.getValue().values());
            spans.sort((a, b) -> Long.compare(a[0], b[0]));
            for (int i = 1; i < spans.size(); i++) {
                if (spans.get(i)[0] < spans.get(i - 1)[1] && spans.get(i - 1)[1] != Long.MAX_VALUE) {
                    info("concurrent-task-execution", e.getKey(), "attempts overlapped by "
                            + (spans.get(i - 1)[1] - spans.get(i)[0]) + " ms");
                    break;
                }
            }
        }
        stats.put("step attempts", attempts.values().stream().mapToInt(Map::size).sum());
        stats.put("step attempts without an end (killed)", attempts.values().stream()
                .flatMap(m -> m.values().stream()).filter(s -> s[1] == Long.MAX_VALUE).count());
        stats.put("steps that succeeded more than once", duplicateSteps);
        stats.put("replay observations", observed.size());
    }

    private void compareObservation(Map<String, Set<String>> observed, String id, String name, Object fromOutput, String key) {
        Set<String> values = observed.get(id + ":" + name);
        if (values == null) {
            return; // the execution's code never ran on a ledger-writing worker
        }
        if (fromOutput != null && values.size() == 1 && !numericEquals(values.iterator().next(), String.valueOf(fromOutput))) {
            error("replay-divergence", key, name + " observed " + values + " but the output has " + fromOutput);
        }
    }

    private static boolean numericEquals(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        try {
            return Double.parseDouble(a) == Double.parseDouble(b);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void checkNondeterminism(Map<String, Map<String, Object>> executions, List<String> workerLogs) {
        executions.forEach((key, wf) -> {
            String reason = String.valueOf(wf.get("reason"));
            if (isDivergence(reason)) {
                error("nondeterminism", key, reason);
            }
            for (Map<String, Object> marker : markers(wf)) {
                int attempts = marker.get("attempts") instanceof Number n ? n.intValue() : 0;
                Object deferred = marker.get("deferred");
                if (attempts > 1 || deferred != null) {
                    info("continuation-retried", key, marker.get("lane") + "#" + marker.get("n") + " leased " + attempts
                            + " time(s)" + (deferred == null ? "" : ", deferred: " + deferred));
                }
                if (deferred != null && isDivergence(String.valueOf(deferred))) {
                    error("nondeterminism", key, marker.get("lane") + "#" + marker.get("n") + ": " + deferred);
                }
                if (marker.get("error") instanceof Map<?, ?> error && isDivergence(String.valueOf(error.get("message")))) {
                    error("nondeterminism", key, marker.get("lane") + "#" + marker.get("n") + ": " + error.get("message"));
                }
            }
        });
        int logged = 0;
        for (String log : workerLogs) {
            logged += (int) log.lines().filter(l -> l.contains("NonDeterminismException")).count();
        }
        if (logged > 0) {
            error("nondeterminism", "worker logs", logged + " NonDeterminismException lines");
        }
    }

    private static boolean isDivergence(String reason) {
        return reason.contains("NonDeterminism") || reason.contains("no longer matches") || reason.contains("diverge")
                || reason.contains("the recorded execution") || reason.contains("the recording");
    }

    // ---- stats ----

    @SuppressWarnings("unchecked")
    private void stats(List<Map<String, Object>> history, List<Map<String, Object>> ledger,
                       Map<String, Map<String, Object>> executions) {
        Map<String, Map<String, Integer>> ops = new TreeMap<>();
        for (Map<String, Object> op : history) {
            if ("nemesis".equals(op.get("process"))) {
                continue;
            }
            ops.computeIfAbsent((String) op.get("f"), f -> new TreeMap<>()).merge((String) op.get("type"), 1, Integer::sum);
        }
        stats.put("operations", ops);
        Map<String, Integer> faults = new TreeMap<>();
        history.stream().filter(op -> "nemesis".equals(op.get("process")) && "start".equals(op.get("type")))
                .forEach(op -> faults.merge((String) op.get("f"), 1, Integer::sum));
        stats.put("faults injected", faults);
        Map<String, Integer> statuses = new TreeMap<>();
        executions.values().forEach(wf -> statuses.merge(String.valueOf(wf.get("status")), 1, Integer::sum));
        stats.put("execution statuses", statuses);
        List<Long> durations = new ArrayList<>();
        for (Map<String, Object> wf : executions.values()) {
            if (wf.get("endTime") instanceof Number end && wf.get("createTime") instanceof Number start && end.longValue() > 0) {
                durations.add(end.longValue() - start.longValue());
            }
        }
        durations.sort(Long::compare);
        if (!durations.isEmpty()) {
            stats.put("execution duration ms p50/p95/max", durations.get(durations.size() / 2) + " / "
                    + durations.get(Math.min(durations.size() - 1, (int) Math.ceil(durations.size() * 0.95) - 1))
                    + " / " + durations.get(durations.size() - 1));
        }
        int continuations = 0;
        List<Long> latencies = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> e : executions.entrySet()) {
            for (Map<String, Object> marker : markers(e.getValue())) {
                continuations++;
                if (marker.get("queuedAt") instanceof Number s && marker.get("appliedAt") instanceof Number end
                        && s.longValue() > 0 && end.longValue() >= s.longValue()) {
                    long ms = end.longValue() - s.longValue();
                    latencies.add(ms);
                    if (ms > 20_000) {
                        info("continuation-stalled", e.getKey(), marker.get("lane") + "#" + marker.get("n") + " took "
                                + ms + " ms (" + marker.get("state") + ")");
                    }
                }
            }
        }
        stats.put("continuations", continuations);
        latencies.sort(Long::compare);
        if (!latencies.isEmpty()) {
            stats.put("continuation latency ms p50/p99/max", latencies.get(latencies.size() / 2) + " / "
                    + latencies.get(Math.min(latencies.size() - 1, (int) Math.ceil(latencies.size() * 0.99) - 1))
                    + " / " + latencies.get(latencies.size() - 1));
        }
    }

    /**
     * Every continuation marker of a summarized execution: the bootstrap marker (and the handler
     * marker, when the class has handlers) from its native state, and the marker of each task the
     * code paused on.
     */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> markers(Map<String, Object> wf) {
        List<Map<String, Object>> markers = new ArrayList<>();
        if (wf.get("nativeState") instanceof Map<?, ?> state) {
            for (String lane : List.of("bootstrap", "handlers")) {
                if (state.get(lane) instanceof Map<?, ?> m) {
                    markers.add((Map<String, Object>) m);
                }
            }
        }
        for (Map<String, Object> t : (List<Map<String, Object>>) wf.getOrDefault("tasks", List.of())) {
            if (t.get("continuation") instanceof Map<?, ?> m) {
                markers.add((Map<String, Object>) m);
            }
        }
        return markers;
    }

    // ---- helpers ----

    private static List<Map<String, Object>> ops(List<Map<String, Object>> history, String f) {
        return history.stream().filter(op -> f.equals(op.get("f")) && !"nemesis".equals(op.get("process"))).toList();
    }

    private void error(String check, String key, String detail) {
        findings.add(new Finding(Level.ERROR, check, key, detail));
    }

    private void info(String check, String key, String detail) {
        findings.add(new Finding(Level.INFO, check, key, detail));
    }

    static List<Map<String, Object>> readLines(Path file) throws IOException {
        List<Map<String, Object>> lines = new ArrayList<>();
        if (!Files.exists(file)) {
            return lines;
        }
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank()) {
                continue;
            }
            try {
                lines.add(JSON.readValue(line, new TypeReference<>() {}));
            } catch (IOException e) {
                // a line cut short by SIGKILL; earlier lines are intact
            }
        }
        return lines;
    }


}
