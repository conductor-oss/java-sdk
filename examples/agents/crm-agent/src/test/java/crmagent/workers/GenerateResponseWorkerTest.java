package crmagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateResponseWorkerTest {

    private final GenerateResponseWorker worker = new GenerateResponseWorker();

    @Test
    void taskDefName() {
        assertEquals("cm_generate_response", worker.getTaskDefName());
    }

    @Test
    void generatesPersonalizedResponse() {
        Task task = taskWith(Map.of(
                "customerName", "Acme Corporation",
                "tier", "enterprise",
                "inquiry", "Our API integration has been experiencing timeout errors",
                "recentIssues", List.of(
                        Map.of("subject", "API rate limiting errors", "status", "resolved"),
                        Map.of("subject", "Invoice discrepancy Q4", "status", "resolved")
                ),
                "sentiment", "positive"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String response = (String) result.getOutputData().get("response");
        assertNotNull(response);
        assertTrue(response.contains("Acme Corporation"));
        assertTrue(response.contains("enterprise"));
        assertTrue(response.contains("timeout errors"));
    }

    @Test
    void setsHighPriorityForEnterprise() {
        Task task = taskWith(Map.of(
                "customerName", "Acme Corporation",
                "tier", "enterprise",
                "inquiry", "Test inquiry",
                "sentiment", "positive"));
        TaskResult result = worker.execute(task);

        assertEquals("high", result.getOutputData().get("priority"));
        assertEquals("personalized", result.getOutputData().get("responseType"));
        assertEquals("4 hours", result.getOutputData().get("estimatedResolution"));
        assertEquals(false, result.getOutputData().get("escalated"));
    }

    @Test
    void setsNormalPriorityForStandard() {
        Task task = taskWith(Map.of(
                "customerName", "Small Corp",
                "tier", "standard",
                "inquiry", "Test inquiry",
                "sentiment", "neutral"));
        TaskResult result = worker.execute(task);

        assertEquals("normal", result.getOutputData().get("priority"));
    }

    @Test
    void includesRecentIssuesInResponse() {
        Task task = taskWith(Map.of(
                "customerName", "Acme Corporation",
                "tier", "enterprise",
                "inquiry", "Test",
                "recentIssues", List.of(
                        Map.of("subject", "API rate limiting errors", "status", "resolved"),
                        Map.of("subject", "Bulk export endpoint", "status", "in_progress")
                ),
                "sentiment", "positive"));
        TaskResult result = worker.execute(task);

        String response = (String) result.getOutputData().get("response");
        assertTrue(response.contains("API rate limiting errors"));
        assertTrue(response.contains("Bulk export endpoint"));
        assertTrue(response.contains("resolved"));
        assertTrue(response.contains("in_progress"));
    }

    @Test
    void handlesNullCustomerName() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerName", null);
        input.put("tier", "enterprise");
        input.put("inquiry", "Test");
        input.put("sentiment", "neutral");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String response = (String) result.getOutputData().get("response");
        assertTrue(response.contains("Valued Customer"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
        assertEquals("normal", result.getOutputData().get("priority"));
    }

    @Test
    void handlesNullRecentIssues() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerName", "Acme Corporation");
        input.put("tier", "enterprise");
        input.put("inquiry", "Test");
        input.put("recentIssues", null);
        input.put("sentiment", "positive");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String response = (String) result.getOutputData().get("response");
        assertNotNull(response);
        assertFalse(response.contains("recent interactions"));
    }

    @Test
    void handlesBlankTier() {
        Task task = taskWith(Map.of(
                "customerName", "Acme Corporation",
                "tier", "  ",
                "inquiry", "Test",
                "sentiment", "positive"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("normal", result.getOutputData().get("priority"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
