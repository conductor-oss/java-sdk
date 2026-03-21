package eventfanout.workers;

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
        assertEquals("fo_receive_event", worker.getTaskDefName());
    }

    @Test
    void receivesEventWithAllInputs() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "eventType", "order.created",
                "payload", Map.of("orderId", "ORD-100", "amount", 99.99)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("evt-001", result.getOutputData().get("eventId"));
        assertEquals("order.created", result.getOutputData().get("eventType"));
        assertNotNull(result.getOutputData().get("payload"));
    }

    @Test
    void passesEventIdThrough() {
        Task task = taskWith(Map.of(
                "eventId", "evt-xyz-999",
                "eventType", "test.event",
                "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("evt-xyz-999", result.getOutputData().get("eventId"));
    }

    @Test
    void passesEventTypeThrough() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "eventType", "payment.received",
                "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("payment.received", result.getOutputData().get("eventType"));
    }

    @Test
    void passesPayloadThrough() {
        Map<String, Object> payload = Map.of("key1", "value1", "key2", 42);
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "eventType", "data.updated",
                "payload", payload));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> outputPayload = (Map<String, Object>) result.getOutputData().get("payload");
        assertEquals("value1", outputPayload.get("key1"));
        assertEquals(42, outputPayload.get("key2"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("eventType", "order.created");
        input.put("payload", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventId"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-004");
        input.put("eventType", null);
        input.put("payload", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-005");
        input.put("eventType", "order.created");
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
        assertEquals("unknown", result.getOutputData().get("eventId"));
        assertEquals("unknown", result.getOutputData().get("eventType"));
        assertNotNull(result.getOutputData().get("payload"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
