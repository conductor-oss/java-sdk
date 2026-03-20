package eventdrivenmicroservices.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmitOrderCreatedWorkerTest {

    private final EmitOrderCreatedWorker worker = new EmitOrderCreatedWorker();

    @Test
    void taskDefName() {
        assertEquals("dm_emit_order_created", worker.getTaskDefName());
    }

    @Test
    void publishesOrderCreatedEvent() {
        Task task = taskWith(Map.of(
                "eventType", "order.created",
                "orderId", "ORD-DM-1001",
                "amount", 199.96));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
        assertEquals("order.created", result.getOutputData().get("eventType"));
    }

    @Test
    void outputContainsPublishedTrue() {
        Task task = taskWith(Map.of(
                "eventType", "order.created",
                "orderId", "ORD-100",
                "amount", 50.0));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("published"));
    }

    @Test
    void outputContainsEventType() {
        Task task = taskWith(Map.of(
                "eventType", "order.created",
                "orderId", "ORD-200",
                "amount", 75.0));
        TaskResult result = worker.execute(task);

        assertEquals("order.created", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesCustomEventType() {
        Task task = taskWith(Map.of(
                "eventType", "order.updated",
                "orderId", "ORD-300",
                "amount", 100.0));
        TaskResult result = worker.execute(task);

        assertEquals("order.updated", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", null);
        input.put("orderId", "ORD-400");
        input.put("amount", 25.0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order.created", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullOrderId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", "order.created");
        input.put("orderId", null);
        input.put("amount", 50.0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
        assertEquals("order.created", result.getOutputData().get("eventType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
