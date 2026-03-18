package agentsupervisor.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReviewWorkerTest {

    private final ReviewWorker worker = new ReviewWorker();

    @Test
    void taskDefName() {
        assertEquals("sup_review", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void producesReviewReport() {
        Task task = buildFullReviewTask();
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertNotNull(report);
        assertEquals("user-authentication", report.get("feature"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void needsRevisionWhenTestsFail() {
        Task task = buildFullReviewTask();
        TaskResult result = worker.execute(task);

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("NEEDS_REVISION", report.get("overallStatus"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsAgentStatuses() {
        Task task = buildFullReviewTask();
        TaskResult result = worker.execute(task);

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("implemented", report.get("codeStatus"));
        assertEquals("needs_fix", report.get("testStatus"));
        assertEquals("complete", report.get("docStatus"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsActionItems() {
        Task task = buildFullReviewTask();
        TaskResult result = worker.execute(task);

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        List<String> actionItems = (List<String>) report.get("actionItems");
        assertNotNull(actionItems);
        assertFalse(actionItems.isEmpty());
        assertTrue(actionItems.stream().anyMatch(a -> a.contains("testTokenExpirationEdgeCase")));
        assertTrue(actionItems.stream().anyMatch(a -> a.contains("coverage")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsMetrics() {
        Task task = buildFullReviewTask();
        TaskResult result = worker.execute(task);

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        Map<String, Object> metrics = (Map<String, Object>) report.get("metrics");
        assertNotNull(metrics);
        assertEquals(245, metrics.get("linesOfCode"));
        assertEquals(18, metrics.get("totalTests"));
        assertEquals(1200, metrics.get("documentationWords"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listsAllAgentsUsed() {
        Task task = buildFullReviewTask();
        TaskResult result = worker.execute(task);

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        List<String> agentsUsed = (List<String>) report.get("agentsUsed");
        assertNotNull(agentsUsed);
        assertEquals(3, agentsUsed.size());
        assertTrue(agentsUsed.contains("coder"));
        assertTrue(agentsUsed.contains("tester"));
        assertTrue(agentsUsed.contains("documenter"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void approvedWhenAllAgentsPass() {
        Map<String, Object> plan = new HashMap<>(Map.of("feature", "search"));
        Map<String, Object> codeResult = new HashMap<>(Map.of("status", "implemented", "linesOfCode", 100));
        Map<String, Object> testResult = new HashMap<>(Map.of("status", "passed", "totalTests", 10));
        Map<String, Object> docResult = new HashMap<>(Map.of("status", "complete", "wordCount", 500));

        Task task = taskWith(new HashMap<>(Map.of(
                "plan", plan,
                "codeResult", codeResult,
                "testResult", testResult,
                "docResult", docResult
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("APPROVED", report.get("overallStatus"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void needsRevisionWhenCodeNotImplemented() {
        Map<String, Object> plan = new HashMap<>(Map.of("feature", "search"));
        Map<String, Object> codeResult = new HashMap<>(Map.of("status", "in_progress", "linesOfCode", 50));
        Map<String, Object> testResult = new HashMap<>(Map.of("status", "passed", "totalTests", 5));
        Map<String, Object> docResult = new HashMap<>(Map.of("status", "complete", "wordCount", 300));

        Task task = taskWith(new HashMap<>(Map.of(
                "plan", plan,
                "codeResult", codeResult,
                "testResult", testResult,
                "docResult", docResult
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("NEEDS_REVISION", report.get("overallStatus"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesNullInputsGracefully() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertNotNull(report);
        assertEquals("unknown", report.get("feature"));
        assertEquals("NEEDS_REVISION", report.get("overallStatus"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void metricsDefaultToZeroWhenMissing() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        Map<String, Object> metrics = (Map<String, Object>) report.get("metrics");
        assertEquals(0, metrics.get("linesOfCode"));
        assertEquals(0, metrics.get("totalTests"));
        assertEquals(0, metrics.get("documentationWords"));
    }

    @Test
    void sameInputProducesSameOutput() {
        Task task1 = buildFullReviewTask();
        Task task2 = buildFullReviewTask();

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("report"), r2.getOutputData().get("report"));
    }

    private Task buildFullReviewTask() {
        Map<String, Object> plan = new HashMap<>(Map.of(
                "feature", "user-authentication",
                "priority", "high",
                "phases", List.of("design", "implementation", "testing", "documentation", "review"),
                "deadline", "2026-03-21"
        ));
        Map<String, Object> codeResult = new HashMap<>(Map.of(
                "filesCreated", List.of("AuthController.java", "AuthService.java", "AuthRepository.java"),
                "linesOfCode", 245,
                "language", "Java",
                "status", "implemented",
                "notes", "Implemented user-authentication"
        ));
        Map<String, Object> testResult = new HashMap<>(Map.of(
                "testSuites", 3,
                "totalTests", 18,
                "passed", 17,
                "failed", 1,
                "coverage", "82%",
                "failedTest", "testTokenExpirationEdgeCase",
                "status", "needs_fix"
        ));
        Map<String, Object> docResult = new HashMap<>(Map.of(
                "documentsCreated", List.of("API_REFERENCE.md", "SETUP_GUIDE.md", "EXAMPLES.md"),
                "sections", List.of("Overview", "Authentication Flow", "API Endpoints", "Configuration", "Troubleshooting"),
                "wordCount", 1200,
                "status", "complete"
        ));

        return taskWith(new HashMap<>(Map.of(
                "plan", plan,
                "codeResult", codeResult,
                "testResult", testResult,
                "docResult", docResult,
                "systemPrompt", "You are a supervisor."
        )));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
