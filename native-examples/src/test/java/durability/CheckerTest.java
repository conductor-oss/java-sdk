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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The checker on synthetic runs: a clean run is valid, and each planted anomaly is reported by the
 * check meant to catch it, and only that check is an error.
 */
class CheckerTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path dir;

    /** A run of one execution that did everything right. */
    private final class Run {
        final List<Map<String, Object>> history = new ArrayList<>();
        final List<Map<String, Object>> ledger = new ArrayList<>();
        final Map<String, List<Map<String, Object>>> byKey = new LinkedHashMap<>();
        final Map<String, Object> byId = new LinkedHashMap<>();
        final Map<String, Object> output = new LinkedHashMap<>();
        final Map<String, Object> wf = new LinkedHashMap<>();

        Run() {
            String key = "k";
            op("starter", "invoke", "start", null);
            op("starter", "ok", "start", "wf-1");
            op("signaler", "invoke", "deposit", "dep-1");
            op("signaler", "ok", "deposit", "dep-1");
            op("signaler", "invoke", "deposit", "dep-2");
            op("signaler", "ok", "deposit", "dep-2");
            op("updater-0", "invoke", "bump", "upd-1");
            op("updater-0", "ok", "bump", Map.of("id", "upd-1", "value", 1));
            for (int step = 1; step <= LedgerWorkflow.STEPS; step++) {
                ledger.add(line("task-start", key + ":" + step, "n" + step, null, 100 + step * 10));
                ledger.add(line("task-end", key + ":" + step, "n" + step, LedgerModel.expectedStep(key, step), 105 + step * 10));
            }
            ledger.add(line("observe", "wf-1:token", null, "tok", 1));
            ledger.add(line("observe", "wf-1:token", null, "tok", 2)); // a replay saw the same value
            output.put("id", key);
            output.put("token", "tok");
            output.put("total", LedgerModel.expectedTotal(key));
            output.put("deposits", new ArrayList<>(List.of("dep-1", "dep-2")));
            output.put("bumps", 1);
            wf.put("workflowId", "wf-1");
            wf.put("status", "COMPLETED");
            wf.put("output", output);
            wf.put("tasks", new ArrayList<>(List.of(Map.of("ref", "durability_step", "type", "durability_step",
                    "status", "COMPLETED", "output", Map.of("result", LedgerModel.expectedStep(key, 1))))));
            byKey.put(key, new ArrayList<>(List.of(wf)));
            byId.put("wf-1", wf);
        }

        void query(String process, List<String> answer) {
            op(process, "invoke", "query", null);
            op(process, "ok", "query", answer);
        }

        void op(String process, String type, String f, Object value) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("t", history.size());
            m.put("process", process);
            m.put("type", type);
            m.put("f", f);
            m.put("key", "k");
            if (value != null) {
                m.put("value", value);
            }
            history.add(m);
        }

        Checker.Report check() throws Exception {
            Files.createDirectories(dir.resolve("ledger"));
            Files.writeString(dir.resolve("history.jsonl"), lines(history));
            Files.writeString(dir.resolve("ledger/ledger-w0-1.jsonl"), lines(ledger));
            Files.writeString(dir.resolve("final.json"), JSON.writeValueAsString(
                    Map.of("byKey", byKey, "byId", byId, "doneAt", Map.of("k", 1))));
            return Checker.check(dir);
        }
    }

    private static Map<String, Object> line(String kind, String key, String nonce, Object value, long t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("t", t);
        m.put("kind", kind);
        m.put("key", key);
        if (nonce != null) {
            m.put("nonce", nonce);
        }
        if (value != null) {
            m.put("value", value);
        }
        return m;
    }

    private static String lines(List<Map<String, Object>> items) throws Exception {
        StringBuilder b = new StringBuilder();
        for (Map<String, Object> m : items) {
            b.append(JSON.writeValueAsString(m)).append('\n');
        }
        return b.toString();
    }

    private void assertOnly(String check, Consumer<Run> plant) throws Exception {
        Run run = new Run();
        plant.accept(run);
        Checker.Report report = run.check();
        assertFalse(report.valid(), check + " was not detected:\n" + report.render());
        List<String> errors = report.findings().stream()
                .filter(f -> f.level() == Checker.Level.ERROR).map(Checker.Finding::check).distinct().toList();
        assertEquals(List.of(check), errors, report.render());
    }

    @Test
    void aCleanRunIsValid() throws Exception {
        Checker.Report report = new Run().check();
        assertTrue(report.valid(), report.render());
    }

    @Test
    void detectsALostSignal() throws Exception {
        assertOnly("lost-signal", r -> ((List<?>) r.output.get("deposits")).remove("dep-2"));
    }

    @Test
    void detectsADuplicateSignal() throws Exception {
        assertOnly("duplicate-signal", r -> r.output.put("deposits", List.of("dep-1", "dep-2", "dep-2")));
    }

    @Test
    void detectsAPhantomSignal() throws Exception {
        assertOnly("phantom-signal", r -> r.output.put("deposits", List.of("dep-1", "dep-2", "dep-9")));
    }

    @Test
    void detectsReorderedSignals() throws Exception {
        assertOnly("signal-reordered", r -> r.output.put("deposits", List.of("dep-2", "dep-1")));
    }

    @Test
    void detectsALostUpdate() throws Exception {
        assertOnly("lost-update", r -> r.output.put("bumps", 0));
    }

    @Test
    void detectsAPhantomUpdate() throws Exception {
        assertOnly("phantom-update", r -> r.output.put("bumps", 2));
    }

    @Test
    void detectsADuplicateExecution() throws Exception {
        assertOnly("duplicate-execution", r -> r.byKey.get("k").add(Map.of("workflowId", "wf-2", "status", "COMPLETED",
                "output", r.output, "tasks", List.of())));
    }

    @Test
    void detectsALostExecution() throws Exception {
        assertOnly("lost-execution", r -> r.byId.put("wf-1", null));
    }

    @Test
    void detectsAnIncompleteExecution() throws Exception {
        assertOnly("incomplete", r -> r.wf.put("status", "RUNNING"));
    }

    @Test
    void detectsAWrongOutput() throws Exception {
        assertOnly("wrong-output", r -> r.output.put("total", 1));
    }

    @Test
    void detectsReplayDivergence() throws Exception {
        assertOnly("replay-divergence", r -> r.ledger.add(line("observe", "wf-1:token", null, "other", 3)));
    }

    @Test
    void detectsATaskThatNeverRan() throws Exception {
        assertOnly("task-never-executed", r -> r.ledger.removeIf(l -> "k:3".equals(l.get("key"))));
    }

    @Test
    void detectsNondeterminism() throws Exception {
        assertOnly("nondeterminism", r -> r.wf.put("tasks", List.of(Map.of("ref", "step_3", "type", "durability_step",
                "status", "COMPLETED", "continuation", Map.of("lane", "main", "n", 3, "state", "APPLIED", "done", true,
                        "error", Map.of("message", "NonDeterminismException: the code no longer matches"))))));
    }

    @Test
    void reportsDuplicateAndConcurrentTaskExecutionAsInformation() throws Exception {
        Run run = new Run();
        run.ledger.add(line("task-start", "k:2", "again", null, 121));
        run.ledger.add(line("task-end", "k:2", "again", LedgerModel.expectedStep("k", 2), 130));
        Checker.Report report = run.check();
        assertTrue(report.valid(), report.render());
        assertEquals(1, report.counts().get("duplicate-task-execution"));
        assertEquals(1, report.counts().get("concurrent-task-execution"));
    }

    @Test
    public void valuesFromAnAttemptThatWasNeverRecordedAreAllowed() throws Exception {
        Run run = new Run();
        // the bootstrap continuation recorded the token when it was applied at t=10
        run.wf.put("nativeState", Map.of("bootstrap", Map.of("lane", "main", "n", 1, "state", "APPLIED", "appliedAt", 10L,
                "recorded", Map.of("token", Map.of("value", "recorded-token", "type", "java.lang.String")))));
        run.ledger.add(0, line("observe", "wf-1:token", null, "from-a-set-aside-attempt", 5));
        Checker.Report report = run.check();
        assertTrue(report.valid(), report.render());
        assertEquals(1, report.counts().get("unrecorded-value-discarded"));

        run.ledger.add(line("observe", "wf-1:token", null, "changed-after-recording", 20));
        assertFalse(run.check().valid());
    }

    @Test
    void queriesThatReadCommittedStateAreValid() throws Exception {
        Run run = new Run();
        run.query("querier-0", List.of());
        run.query("querier-0", List.of("dep-1"));
        run.query("final-querier", List.of("dep-1", "dep-2"));
        Checker.Report report = run.check();
        assertTrue(report.valid(), report.render());
    }

    @Test
    void detectsAQueryAnswerThatIsNotAPrefix() throws Exception {
        assertOnly("query-not-prefix", r -> r.query("querier-0", List.of("dep-2")));
    }

    @Test
    void detectsAQueryThatWentBack() throws Exception {
        assertOnly("query-went-back", r -> {
            r.query("querier-0", List.of("dep-1", "dep-2"));
            r.query("querier-0", List.of("dep-1"));
        });
    }

    @Test
    void detectsAFinalQueryThatDiffersFromTheOutput() throws Exception {
        assertOnly("query-final-mismatch", r -> r.query("final-querier", List.of("dep-1")));
    }

    @Test
    void detectsAQueryWriteReachingTheExecution() throws Exception {
        Run run = new Run();
        run.output.put("deposits", new ArrayList<>(List.of("dep-1", "dep-2", LedgerWorkflow.QUERY_MARKER)));
        Checker.Report report = run.check();
        // also a phantom signal: nobody sent the marker
        assertEquals(1, report.counts().get("query-mutated"), report.render());
    }

    @Test
    void detectsUpdatesAppliedOutOfRealTimeOrder() throws Exception {
        // upd-1 returned 1 earlier; now an update acknowledged first returns a larger value than a later one
        assertOnly("update-order", r -> {
            r.op("updater-1", "invoke", "bump", "upd-2");
            r.op("updater-1", "ok", "bump", Map.of("id", "upd-2", "value", 3));
            r.op("updater-2", "invoke", "bump", "upd-3");
            r.op("updater-2", "ok", "bump", Map.of("id", "upd-3", "value", 2));
            r.output.put("bumps", 3);
        });
    }
}
