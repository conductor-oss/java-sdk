package eventcorrelation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveOrderWorkerTest {

    private final ReceiveOrderWorker worker = new ReceiveOrderWorker();

    @Test
    void taskDefName() {
        assertEquals("ec_receive_order", worker.getTaskDefName());
    }

    @Test
    void returnsOrderEvent() {
        Task task = taskWith(Map.of("correlationId", "corr-fixed-001", "eventType", "order"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertNotNull(event);
        assertEquals("order", event.get("type"));
        assertEquals("ORD-7712", event.get("orderId"));
    }

    @Test
    void orderEventContainsCustomerId() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "order"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals("C-001", event.get("customerId"));
    }

    @Test
    void orderEventContainsAmount() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "order"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals(599.99, event.get("amount"));
        assertEquals("USD", event.get("currency"));
    }

    @Test
    void orderEventContainsTimestamp() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "order"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals("2026-01-15T10:00:00Z", event.get("timestamp"));
    }

    @Test
    void eventContainsAllExpectedFields() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "order"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals(6, event.size());
        assertTrue(event.containsKey("type"));
        assertTrue(event.containsKey("orderId"));
        assertTrue(event.containsKey("customerId"));
        assertTrue(event.containsKey("amount"));
        assertTrue(event.containsKey("currency"));
        assertTrue(event.containsKey("timestamp"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("event"));
    }

    @Test
    void outputContainsEventKey() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "order"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("event"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
