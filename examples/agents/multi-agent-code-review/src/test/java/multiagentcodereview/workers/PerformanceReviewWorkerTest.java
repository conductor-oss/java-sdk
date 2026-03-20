package multiagentcodereview.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceReviewWorkerTest {

    private final PerformanceReviewWorker worker = new PerformanceReviewWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_performance_review", worker.getTaskDefName());
    }

    @Test
    void returnsTwoFindings() {
        Task task = taskWith(Map.of(
                "ast", Map.of("functions", List.of("f1"), "lines", 100),
                "language", "javascript"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings =
                (List<Map<String, Object>>) result.getOutputData().get("findings");
        assertNotNull(findings);
        assertEquals(2, findings.size());
    }

    @Test
    void findingsContainExpectedTypes() {
        Task task = taskWith(Map.of(
                "ast", Map.of("functions", List.of("f1"), "lines", 100),
                "language", "javascript"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings =
                (List<Map<String, Object>>) result.getOutputData().get("findings");

        assertEquals("N_PLUS_1_QUERY", findings.get(0).get("type"));
        assertEquals("HIGH", findings.get(0).get("severity"));

        assertEquals("NO_CONNECTION_POOL", findings.get(1).get("type"));
        assertEquals("MEDIUM", findings.get(1).get("severity"));
    }

    @Test
    void findingsContainLineNumbersAndMessages() {
        Task task = taskWith(Map.of(
                "ast", Map.of("functions", List.of("f1"), "lines", 100),
                "language", "javascript"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings =
                (List<Map<String, Object>>) result.getOutputData().get("findings");

        for (Map<String, Object> finding : findings) {
            assertNotNull(finding.get("line"));
            assertNotNull(finding.get("message"));
            assertTrue(((String) finding.get("message")).length() > 10);
        }
    }

    @Test
    void outputIncludesAgentName() {
        Task task = taskWith(Map.of(
                "ast", Map.of("functions", List.of("f1"), "lines", 100),
                "language", "javascript"));
        TaskResult result = worker.execute(task);

        assertEquals("performance", result.getOutputData().get("agent"));
    }

    @Test
    void handlesNullAst() {
        Map<String, Object> input = new HashMap<>();
        input.put("ast", null);
        input.put("language", "javascript");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("findings"));
    }

    @Test
    void handlesMissingLanguage() {
        Task task = taskWith(Map.of("ast", Map.of("functions", List.of("f1"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("performance", result.getOutputData().get("agent"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
