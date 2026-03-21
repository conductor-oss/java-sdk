package webhooktrigger.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessEventWorkerTest {

    private final ProcessEventWorker worker = new ProcessEventWorker();

    @Test
    void taskDefName() {
        assertEquals("wt_process_event", worker.getTaskDefName());
    }

    @Test
    void processesOrderCreatedEvent() {
        Task task = taskWith(new HashMap<>(Map.of(
                "eventType", "order.created",
                "payload", Map.of("orderId", "ORD-2026-4821"),
                "source", "shopify-webhook",
                "timestamp", "2026-03-08T10:00:00Z")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order.created", result.getOutputData().get("eventType"));
    }

    @Test
    void returnsParsedData() {
        Task task = taskWith(new HashMap<>(Map.of(
                "eventType", "order.created",
                "payload", Map.of("orderId", "ORD-2026-4821"),
                "source", "shopify-webhook",
                "timestamp", "2026-03-08T10:00:00Z")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedData = (Map<String, Object>) result.getOutputData().get("parsedData");
        assertNotNull(parsedData);
        assertEquals("ORD-2026-4821", parsedData.get("orderId"));
        assertEquals("acme-corp", parsedData.get("customer"));
        assertEquals(1250.00, parsedData.get("amount"));
        assertEquals("USD", parsedData.get("currency"));
        assertEquals(3, parsedData.get("items"));
    }

    @Test
    void returnsSchema() {
        Task task = taskWith(new HashMap<>(Map.of(
                "eventType", "order.created",
                "payload", Map.of(),
                "source", "shopify-webhook",
                "timestamp", "2026-03-08T10:00:00Z")));
        TaskResult result = worker.execute(task);

        assertEquals("order_v2", result.getOutputData().get("schema"));
    }

    @Test
    void returnsFixedReceivedAt() {
        Task task = taskWith(new HashMap<>(Map.of(
                "eventType", "order.created",
                "payload", Map.of(),
                "source", "shopify-webhook",
                "timestamp", "2026-03-08T10:00:00Z")));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("receivedAt"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", null);
        input.put("payload", Map.of());
        input.put("source", "test");
        input.put("timestamp", "2026-03-08T10:00:00Z");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesEmptyEventType() {
        Task task = taskWith(new HashMap<>(Map.of(
                "eventType", "",
                "payload", Map.of(),
                "source", "test",
                "timestamp", "2026-03-08T10:00:00Z")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventType"));
        assertNotNull(result.getOutputData().get("parsedData"));
        assertNotNull(result.getOutputData().get("schema"));
        assertNotNull(result.getOutputData().get("receivedAt"));
    }

    @Test
    void preservesEventTypeInOutput() {
        Task task = taskWith(new HashMap<>(Map.of(
                "eventType", "payment.received",
                "payload", Map.of(),
                "source", "stripe",
                "timestamp", "2026-03-08T10:00:00Z")));
        TaskResult result = worker.execute(task);

        assertEquals("payment.received", result.getOutputData().get("eventType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
