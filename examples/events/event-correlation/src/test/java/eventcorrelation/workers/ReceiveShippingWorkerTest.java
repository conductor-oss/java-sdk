package eventcorrelation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveShippingWorkerTest {

    private final ReceiveShippingWorker worker = new ReceiveShippingWorker();

    @Test
    void taskDefName() {
        assertEquals("ec_receive_shipping", worker.getTaskDefName());
    }

    @Test
    void returnsShippingEvent() {
        Task task = taskWith(Map.of("correlationId", "corr-fixed-001", "eventType", "shipping"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertNotNull(event);
        assertEquals("shipping", event.get("type"));
        assertEquals("SHP-5501", event.get("shipmentId"));
    }

    @Test
    void shippingEventContainsOrderId() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "shipping"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals("ORD-7712", event.get("orderId"));
    }

    @Test
    void shippingEventContainsCarrier() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "shipping"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals("FedEx", event.get("carrier"));
        assertEquals("FX-998877", event.get("trackingNumber"));
    }

    @Test
    void shippingEventContainsTimestamp() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "shipping"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals("2026-01-15T10:02:00Z", event.get("timestamp"));
    }

    @Test
    void eventContainsAllExpectedFields() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "shipping"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals(6, event.size());
        assertTrue(event.containsKey("type"));
        assertTrue(event.containsKey("shipmentId"));
        assertTrue(event.containsKey("orderId"));
        assertTrue(event.containsKey("carrier"));
        assertTrue(event.containsKey("trackingNumber"));
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
        Task task = taskWith(Map.of("correlationId", "corr-1", "eventType", "shipping"));
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
