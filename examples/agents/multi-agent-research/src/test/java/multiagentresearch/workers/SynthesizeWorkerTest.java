package multiagentresearch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SynthesizeWorkerTest {

    private final SynthesizeWorker worker = new SynthesizeWorker();

    @Test
    void taskDefName() {
        assertEquals("ra_synthesize", worker.getTaskDefName());
    }

    @Test
    void synthesizesAllFindings() {
        List<Map<String, Object>> webFindings = List.of(
                Map.of("source", "web1", "credibility", 0.87),
                Map.of("source", "web2", "credibility", 0.92),
                Map.of("source", "web3", "credibility", 0.78));
        List<Map<String, Object>> paperFindings = List.of(
                Map.of("source", "paper1", "credibility", 0.95),
                Map.of("source", "paper2", "credibility", 0.93));
        List<Map<String, Object>> dbFindings = List.of(
                Map.of("source", "db1", "credibility", 0.91),
                Map.of("source", "db2", "credibility", 0.85),
                Map.of("source", "db3", "credibility", 0.88));

        Task task = taskWith(Map.of(
                "topic", "AI in software engineering",
                "webFindings", webFindings,
                "paperFindings", paperFindings,
                "dbFindings", dbFindings));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(8, result.getOutputData().get("totalSources"));
    }

    @Test
    void synthesisContainsExpectedFields() {
        Task task = taskWith(Map.of(
                "topic", "test topic",
                "webFindings", List.of(Map.of("credibility", 0.9)),
                "paperFindings", List.of(Map.of("credibility", 0.85)),
                "dbFindings", List.of(Map.of("credibility", 0.88))));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> synthesis = (Map<String, Object>) result.getOutputData().get("synthesis");
        assertNotNull(synthesis);
        assertEquals("test topic", synthesis.get("topic"));
        assertTrue(synthesis.containsKey("webSourceCount"));
        assertTrue(synthesis.containsKey("paperSourceCount"));
        assertTrue(synthesis.containsKey("dbSourceCount"));
        assertTrue(synthesis.containsKey("avgCredibility"));
        assertTrue(synthesis.containsKey("convergenceLevel"));
    }

    @Test
    void returnsFourKeyInsights() {
        Task task = taskWith(Map.of(
                "topic", "test",
                "webFindings", List.of(Map.of("credibility", 0.8)),
                "paperFindings", List.of(),
                "dbFindings", List.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> keyInsights = (List<String>) result.getOutputData().get("keyInsights");
        assertNotNull(keyInsights);
        assertEquals(4, keyInsights.size());
        for (String insight : keyInsights) {
            assertFalse(insight.isEmpty());
        }
    }

    @Test
    void confidenceBasedOnCredibility() {
        Task task = taskWith(Map.of(
                "topic", "test",
                "webFindings", List.of(Map.of("credibility", 0.9)),
                "paperFindings", List.of(Map.of("credibility", 0.8)),
                "dbFindings", List.of()));
        TaskResult result = worker.execute(task);

        double confidence = ((Number) result.getOutputData().get("confidence")).doubleValue();
        assertTrue(confidence > 0.0);
        assertTrue(confidence <= 1.0);
    }

    @Test
    void confidenceIsFiftyWhenNoSources() {
        Task task = taskWith(Map.of("topic", "test"));
        TaskResult result = worker.execute(task);

        double confidence = ((Number) result.getOutputData().get("confidence")).doubleValue();
        assertEquals(0.5, confidence, 0.001);
    }

    @Test
    void convergenceLevelHighWhenCredibilityAbove85() {
        Task task = taskWith(Map.of(
                "topic", "test",
                "webFindings", List.of(Map.of("credibility", 0.90)),
                "paperFindings", List.of(Map.of("credibility", 0.92)),
                "dbFindings", List.of(Map.of("credibility", 0.88))));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> synthesis = (Map<String, Object>) result.getOutputData().get("synthesis");
        assertEquals("high", synthesis.get("convergenceLevel"));
    }

    @Test
    void convergenceLevelModerateWhenCredibilityBetween70And85() {
        Task task = taskWith(Map.of(
                "topic", "test",
                "webFindings", List.of(Map.of("credibility", 0.75)),
                "paperFindings", List.of(Map.of("credibility", 0.80)),
                "dbFindings", List.of(Map.of("credibility", 0.72))));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> synthesis = (Map<String, Object>) result.getOutputData().get("synthesis");
        assertEquals("moderate", synthesis.get("convergenceLevel"));
    }

    @Test
    void handlesNullFindings() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", "test");
        input.put("webFindings", null);
        input.put("paperFindings", null);
        input.put("dbFindings", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalSources"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("synthesis"));
    }

    @Test
    void computeAverageCredibilityCorrectly() {
        List<Map<String, Object>> web = List.of(Map.of("credibility", 0.80));
        List<Map<String, Object>> papers = List.of(Map.of("credibility", 0.90));
        List<Map<String, Object>> db = List.of(Map.of("credibility", 1.0));

        double avg = worker.computeAverageCredibility(web, papers, db);
        assertEquals(0.9, avg, 0.001);
    }

    @Test
    void computeAverageCredibilityReturnsZeroForEmpty() {
        double avg = worker.computeAverageCredibility(List.of(), List.of(), List.of());
        assertEquals(0.0, avg, 0.001);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
