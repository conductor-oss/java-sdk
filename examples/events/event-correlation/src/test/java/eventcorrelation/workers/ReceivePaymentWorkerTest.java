package eventcorrelation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceivePaymentWorkerTest {

    private final ReceivePaymentWorker worker = new ReceivePaymentWorker();

    @Test
    void taskDefName() {
        assertEquals("ec_receive_payment", worker.getTaskDefName());
    }

    @Test
    void returnsPaymentEvent() {
        Task task = taskWith(Map.of("correlationId", "corr-fixed-001", "eventType", "payment"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertNotNull(event);
        assertEquals("payment", event.get("type"));
        assertEquals("PAY-3301", event.get("paymentId"));
    }

    @Test
    void paymentEventContainsOrderId() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "payment"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals("ORD-7712", event.get("orderId"));
    }

    @Test
    void paymentEventContainsAmount() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "payment"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals(599.99, event.get("amount"));
        assertEquals("credit_card", event.get("method"));
    }

    @Test
    void paymentEventContainsTimestamp() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "payment"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals("2026-01-15T10:01:00Z", event.get("timestamp"));
    }

    @Test
    void eventContainsAllExpectedFields() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "payment"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals(6, event.size());
        assertTrue(event.containsKey("type"));
        assertTrue(event.containsKey("paymentId"));
        assertTrue(event.containsKey("orderId"));
        assertTrue(event.containsKey("amount"));
        assertTrue(event.containsKey("method"));
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
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "payment"));
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
