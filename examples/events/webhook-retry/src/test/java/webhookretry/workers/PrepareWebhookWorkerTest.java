package webhookretry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrepareWebhookWorkerTest {

    private final PrepareWebhookWorker worker = new PrepareWebhookWorker();

    @Test
    void taskDefName() {
        assertEquals("wr_prepare_webhook", worker.getTaskDefName());
    }

    @Test
    void preparesWebhookWithValidInput() {
        Task task = taskWith(Map.of(
                "webhookUrl", "https://api.example.com/webhook",
                "payload", Map.of("event", "order.completed")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("https://api.example.com/webhook", result.getOutputData().get("url"));
        assertNotNull(result.getOutputData().get("payload"));
    }

    @Test
    void outputContainsPreparedAtTimestamp() {
        Task task = taskWith(Map.of(
                "webhookUrl", "https://hooks.example.com/notify",
                "payload", Map.of("type", "alert")));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("preparedAt"));
    }

    @Test
    void passesPayloadThrough() {
        Map<String, Object> payload = Map.of("orderId", "ORD-123", "amount", 50);
        Task task = taskWith(Map.of(
                "webhookUrl", "https://api.example.com/webhook",
                "payload", payload));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> outputPayload = (Map<String, Object>) result.getOutputData().get("payload");
        assertEquals("ORD-123", outputPayload.get("orderId"));
        assertEquals(50, outputPayload.get("amount"));
    }

    @Test
    void handlesNullWebhookUrl() {
        Map<String, Object> input = new HashMap<>();
        input.put("webhookUrl", null);
        input.put("payload", Map.of("event", "test"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("https://default.example.com/webhook", result.getOutputData().get("url"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("webhookUrl", "https://api.example.com/webhook");
        input.put("payload", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("payload"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("https://default.example.com/webhook", result.getOutputData().get("url"));
        assertNotNull(result.getOutputData().get("payload"));
        assertNotNull(result.getOutputData().get("preparedAt"));
    }

    @Test
    void preservesWebhookUrlInOutput() {
        Task task = taskWith(Map.of(
                "webhookUrl", "https://custom.endpoint.io/hook",
                "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("https://custom.endpoint.io/hook", result.getOutputData().get("url"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
