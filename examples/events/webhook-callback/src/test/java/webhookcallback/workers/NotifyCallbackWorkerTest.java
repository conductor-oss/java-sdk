package webhookcallback.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotifyCallbackWorkerTest {

    private final NotifyCallbackWorker worker = new NotifyCallbackWorker();

    @Test
    void taskDefName() {
        assertEquals("wc_notify_callback", worker.getTaskDefName());
    }

    @Test
    void sendsCallback() {
        Task task = taskWith(Map.of(
                "callbackUrl", "https://api.partner.com/webhooks/completion",
                "requestId", "req-fixed-001",
                "result", Map.of("recordsProcessed", 150, "recordsSucceeded", 148, "recordsFailed", 2),
                "status", "completed"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("callbackSent"));
        assertEquals(200, result.getOutputData().get("responseStatus"));
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("sentAt"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsCallbackPayload() {
        Task task = taskWith(Map.of(
                "callbackUrl", "https://api.partner.com/webhooks/completion",
                "requestId", "req-001",
                "result", Map.of("recordsProcessed", 150),
                "status", "completed"));
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("callbackPayload");
        assertNotNull(payload);
        assertEquals("req-001", payload.get("requestId"));
        assertEquals("completed", payload.get("status"));
        assertEquals("2026-01-15T10:00:00Z", payload.get("completedAt"));
        assertNotNull(payload.get("result"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void payloadContainsProcessingResult() {
        Map<String, Object> processingResult = Map.of(
                "recordsProcessed", 150,
                "recordsSucceeded", 148,
                "recordsFailed", 2);
        Task task = taskWith(Map.of(
                "callbackUrl", "https://example.com/callback",
                "requestId", "req-002",
                "result", processingResult,
                "status", "completed"));
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("callbackPayload");
        Map<String, Object> embeddedResult = (Map<String, Object>) payload.get("result");
        assertEquals(150, embeddedResult.get("recordsProcessed"));
        assertEquals(148, embeddedResult.get("recordsSucceeded"));
        assertEquals(2, embeddedResult.get("recordsFailed"));
    }

    @Test
    void handlesMissingCallbackUrl() {
        Task task = taskWith(Map.of(
                "requestId", "req-003",
                "result", Map.of("recordsProcessed", 50),
                "status", "completed"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("callbackSent"));
    }

    @Test
    void handlesMissingRequestId() {
        Task task = taskWith(Map.of(
                "callbackUrl", "https://example.com/callback",
                "result", Map.of(),
                "status", "completed"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("callbackPayload");
        assertEquals("unknown", payload.get("requestId"));
    }

    @Test
    void handlesMissingStatus() {
        Task task = taskWith(Map.of(
                "callbackUrl", "https://example.com/callback",
                "requestId", "req-004",
                "result", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("callbackPayload");
        assertEquals("unknown", payload.get("status"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("callbackSent"));
        assertEquals(200, result.getOutputData().get("responseStatus"));
        assertNotNull(result.getOutputData().get("callbackPayload"));
    }

    @Test
    void handlesNullValues() {
        Map<String, Object> input = new HashMap<>();
        input.put("callbackUrl", null);
        input.put("requestId", null);
        input.put("result", null);
        input.put("status", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("callbackSent"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
