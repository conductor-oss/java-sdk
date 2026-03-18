package agenthandoff.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TechWorkerTest {

    private final TechWorker worker = new TechWorker();

    @Test
    void taskDefName() {
        assertEquals("ah_tech", worker.getTaskDefName());
    }

    // --- Root cause analysis by message content ---

    @Test
    void diagnosesTimeoutIssue() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-200",
                "message", "API requests timeout after 30 seconds",
                "triageNotes", "Technical issue detected",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String resolution = (String) result.getOutputData().get("resolution");
        assertTrue(resolution.contains("latency"));
        assertTrue(resolution.contains("CUST-200"));

        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) result.getOutputData().get("diagnostics");
        assertEquals("upstream_timeout", diagnostics.get("rootCause"));
        assertEquals(4200, diagnostics.get("apiLatencyMs"));
    }

    @Test
    void diagnosesServerError() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-201",
                "message", "Getting 500 error on every request",
                "triageNotes", "Tech issue",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) result.getOutputData().get("diagnostics");
        assertEquals("bad_deployment", diagnostics.get("rootCause"));
        assertEquals("12.4%", diagnostics.get("errorRate"));
    }

    @Test
    void diagnosesRateLimiting() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-202",
                "message", "I keep getting rate limit errors",
                "triageNotes", "Tech issue",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) result.getOutputData().get("diagnostics");
        assertEquals("rate_limit_exceeded", diagnostics.get("rootCause"));
        assertEquals(500, diagnostics.get("rateLimitRemaining"));
    }

    @Test
    void unclassifiedWhenNoSpecificKeywords() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-203",
                "message", "Something is wrong with my API",
                "triageNotes", "Tech issue",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) result.getOutputData().get("diagnostics");
        assertEquals("pending_investigation", diagnostics.get("rootCause"));
    }

    // --- Urgency affects diagnostics ---

    @Test
    void highUrgencySetsPriorityP1() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-210",
                "message", "API timeout",
                "triageNotes", "Tech issue",
                "urgency", "high"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) result.getOutputData().get("diagnostics");
        assertEquals("P1", diagnostics.get("priority"));
    }

    @Test
    void normalUrgencyDoesNotSetPriority() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-211",
                "message", "API timeout",
                "triageNotes", "Tech issue",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) result.getOutputData().get("diagnostics");
        assertFalse(diagnostics.containsKey("priority"));
    }

    // --- Ticket ID ---

    @Test
    void ticketIdIsDeterministicPerCustomer() {
        Task task1 = taskWith(Map.of("customerId", "CUST-220", "message", "error", "triageNotes", "", "urgency", "normal"));
        Task task2 = taskWith(Map.of("customerId", "CUST-220", "message", "timeout", "triageNotes", "", "urgency", "normal"));

        String ticket1 = (String) worker.execute(task1).getOutputData().get("ticketId");
        String ticket2 = (String) worker.execute(task2).getOutputData().get("ticketId");

        assertTrue(ticket1.startsWith("TECH-"));
        assertEquals(ticket1, ticket2);
    }

    // --- Output shape ---

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-204",
                "message", "API problem",
                "triageNotes", "notes",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("specialist"));
        assertTrue(result.getOutputData().containsKey("ticketId"));
        assertTrue(result.getOutputData().containsKey("resolution"));
        assertTrue(result.getOutputData().containsKey("diagnostics"));
        assertEquals(4, result.getOutputData().size());
        assertEquals("technical", result.getOutputData().get("specialist"));
    }

    // --- Null / blank / missing inputs ---

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", null);
        input.put("message", null);
        input.put("triageNotes", null);
        input.put("urgency", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("resolution"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("diagnostics"));
    }

    @Test
    void handlesBlankInputs() {
        Task task = taskWith(Map.of(
                "customerId", "   ",
                "message", "   ",
                "triageNotes", "",
                "urgency", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
