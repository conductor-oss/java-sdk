package eventsplit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveCompositeWorkerTest {

    private final ReceiveCompositeWorker worker = new ReceiveCompositeWorker();

    @Test
    void taskDefName() {
        assertEquals("sp_receive_composite", worker.getTaskDefName());
    }

    @Test
    void receivesCompositeEvent() {
        Map<String, Object> compositeEvent = Map.of(
                "type", "purchase_complete",
                "order", Map.of("orderId", "ORD-700"),
                "customer", Map.of("id", "CUST-88"),
                "shipping", Map.of("method", "express"));
        Task task = taskWith(Map.of("compositeEvent", compositeEvent));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(compositeEvent, result.getOutputData().get("event"));
    }

    @Test
    void outputContainsEventKey() {
        Task task = taskWith(Map.of("compositeEvent", Map.of("type", "test")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("event"));
    }

    @Test
    void passesCompositeEventAsEvent() {
        Map<String, Object> compositeEvent = Map.of("type", "order_batch", "count", 5);
        Task task = taskWith(Map.of("compositeEvent", compositeEvent));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertEquals("order_batch", event.get("type"));
        assertEquals(5, event.get("count"));
    }

    @Test
    void handlesNullCompositeEvent() {
        Map<String, Object> input = new HashMap<>();
        input.put("compositeEvent", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("event"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("event"));
    }

    @Test
    void handlesEmptyCompositeEvent() {
        Task task = taskWith(Map.of("compositeEvent", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        assertTrue(event.isEmpty());
    }

    @Test
    void preservesNestedEventStructure() {
        Map<String, Object> compositeEvent = Map.of(
                "type", "purchase_complete",
                "order", Map.of("orderId", "ORD-999", "items", 7, "total", 549.99));
        Task task = taskWith(Map.of("compositeEvent", compositeEvent));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) result.getOutputData().get("event");
        @SuppressWarnings("unchecked")
        Map<String, Object> order = (Map<String, Object>) event.get("order");
        assertEquals("ORD-999", order.get("orderId"));
        assertEquals(7, order.get("items"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
