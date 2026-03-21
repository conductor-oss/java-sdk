package multiagentcodereview.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompileReviewWorkerTest {

    private final CompileReviewWorker worker = new CompileReviewWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_compile_review", worker.getTaskDefName());
    }

    @Test
    void compilesAllFindings() {
        List<Map<String, Object>> secFindings = List.of(
                Map.of("severity", "HIGH", "type", "SQL_INJECTION"),
                Map.of("severity", "MEDIUM", "type", "WEAK_CRYPTO"),
                Map.of("severity", "LOW", "type", "MISSING_HELMET"));
        List<Map<String, Object>> perfFindings = List.of(
                Map.of("severity", "HIGH", "type", "N_PLUS_1_QUERY"),
                Map.of("severity", "MEDIUM", "type", "NO_CONNECTION_POOL"));
        List<Map<String, Object>> styleFindings = List.of(
                Map.of("severity", "LOW", "type", "INCONSISTENT_NAMING"),
                Map.of("severity", "LOW", "type", "MISSING_JSDOC"),
                Map.of("severity", "LOW", "type", "LONG_FUNCTION"));

        Task task = taskWith(Map.of(
                "securityFindings", secFindings,
                "performanceFindings", perfFindings,
                "styleFindings", styleFindings));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(8, result.getOutputData().get("totalIssues"));
        assertEquals(2, result.getOutputData().get("highCount"));
        assertEquals("CRITICAL", result.getOutputData().get("overallSeverity"));
    }

    @Test
    void summaryContainsAllCounts() {
        List<Map<String, Object>> secFindings = List.of(
                Map.of("severity", "HIGH", "type", "SQL_INJECTION"),
                Map.of("severity", "MEDIUM", "type", "WEAK_CRYPTO"),
                Map.of("severity", "LOW", "type", "MISSING_HELMET"));
        List<Map<String, Object>> perfFindings = List.of(
                Map.of("severity", "HIGH", "type", "N_PLUS_1_QUERY"),
                Map.of("severity", "MEDIUM", "type", "NO_CONNECTION_POOL"));
        List<Map<String, Object>> styleFindings = List.of(
                Map.of("severity", "LOW", "type", "INCONSISTENT_NAMING"),
                Map.of("severity", "LOW", "type", "MISSING_JSDOC"),
                Map.of("severity", "LOW", "type", "LONG_FUNCTION"));

        Task task = taskWith(Map.of(
                "securityFindings", secFindings,
                "performanceFindings", perfFindings,
                "styleFindings", styleFindings));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertTrue(summary.contains("8 issues"));
        assertTrue(summary.contains("2 high severity"));
        assertTrue(summary.contains("Security: 3"));
        assertTrue(summary.contains("Performance: 2"));
        assertTrue(summary.contains("Style: 3"));
        assertTrue(summary.contains("CRITICAL"));
    }

    @Test
    void singleHighSeverityReturnsHigh() {
        List<Map<String, Object>> secFindings = List.of(
                Map.of("severity", "HIGH", "type", "SQL_INJECTION"));
        List<Map<String, Object>> perfFindings = List.of(
                Map.of("severity", "MEDIUM", "type", "NO_CONNECTION_POOL"));
        List<Map<String, Object>> styleFindings = List.of(
                Map.of("severity", "LOW", "type", "LONG_FUNCTION"));

        Task task = taskWith(Map.of(
                "securityFindings", secFindings,
                "performanceFindings", perfFindings,
                "styleFindings", styleFindings));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("totalIssues"));
        assertEquals(1, result.getOutputData().get("highCount"));
        assertEquals("HIGH", result.getOutputData().get("overallSeverity"));
    }

    @Test
    void noHighSeverityReturnsLow() {
        List<Map<String, Object>> secFindings = List.of(
                Map.of("severity", "MEDIUM", "type", "WEAK_CRYPTO"));
        List<Map<String, Object>> perfFindings = List.of(
                Map.of("severity", "MEDIUM", "type", "NO_CONNECTION_POOL"));
        List<Map<String, Object>> styleFindings = List.of(
                Map.of("severity", "LOW", "type", "LONG_FUNCTION"));

        Task task = taskWith(Map.of(
                "securityFindings", secFindings,
                "performanceFindings", perfFindings,
                "styleFindings", styleFindings));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("totalIssues"));
        assertEquals(0, result.getOutputData().get("highCount"));
        assertEquals("LOW", result.getOutputData().get("overallSeverity"));
    }

    @Test
    void handlesNullFindings() {
        Map<String, Object> input = new HashMap<>();
        input.put("securityFindings", null);
        input.put("performanceFindings", null);
        input.put("styleFindings", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalIssues"));
        assertEquals(0, result.getOutputData().get("highCount"));
        assertEquals("LOW", result.getOutputData().get("overallSeverity"));
    }

    @Test
    void handlesMissingFindings() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalIssues"));
        assertEquals(0, result.getOutputData().get("highCount"));
    }

    @Test
    void handlesEmptyFindings() {
        Task task = taskWith(Map.of(
                "securityFindings", List.of(),
                "performanceFindings", List.of(),
                "styleFindings", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalIssues"));
        assertEquals(0, result.getOutputData().get("highCount"));
        assertEquals("LOW", result.getOutputData().get("overallSeverity"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
