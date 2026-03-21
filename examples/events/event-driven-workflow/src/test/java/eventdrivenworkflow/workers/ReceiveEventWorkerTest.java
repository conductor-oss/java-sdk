package eventdrivenworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveEventWorkerTest {

    private final ReceiveEventWorker worker = new ReceiveEventWorker();

    @Test
    void taskDefName() {
        assertEquals("ed_receive_event", worker.getTaskDefName());
    }

    @Test
    void receivesOrderCreatedEvent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "eventType", "order.created",
                "eventData", Map.of("orderId", "ORD-100", "amount", 99.99)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order.created", result.getOutputData().get("eventType"));
        assertNotNull(result.getOutputData().get("eventData"));
        assertNotNull(result.getOutputData().get("metadata"));
    }

    @Test
    void outputContainsMetadataWithSource() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "eventType", "payment.received",
                "eventData", Map.of("paymentId", "PAY-200")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, String> metadata = (Map<String, String>) result.getOutputData().get("metadata");
        assertEquals("event-bus", metadata.get("source"));
        assertEquals("2026-01-15T10:00:00Z", metadata.get("receivedAt"));
    }

    @Test
    void passesEventDataThrough() {
        Map<String, Object> eventData = Map.of("orderId", "ORD-555", "amount", 42.00);
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "eventType", "order.updated",
                "eventData", eventData));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> outputEventData = (Map<String, Object>) result.getOutputData().get("eventData");
        assertEquals("ORD-555", outputEventData.get("orderId"));
    }

    @Test
    void passesEventTypeThrough() {
        Task task = taskWith(Map.of(
                "eventId", "evt-004",
                "eventType", "payment.refunded",
                "eventData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("payment.refunded", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-005");
        input.put("eventType", null);
        input.put("eventData", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullEventData() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-006");
        input.put("eventType", "order.created");
        input.put("eventData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("eventData"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventType"));
        assertNotNull(result.getOutputData().get("metadata"));
    }

    @Test
    void handlesCustomEventType() {
        Task task = taskWith(Map.of(
                "eventId", "evt-007",
                "eventType", "inventory.low",
                "eventData", Map.of("sku", "ITEM-42")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("inventory.low", result.getOutputData().get("eventType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
