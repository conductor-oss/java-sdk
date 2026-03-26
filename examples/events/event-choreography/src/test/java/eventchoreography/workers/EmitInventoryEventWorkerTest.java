package eventchoreography.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmitInventoryEventWorkerTest {

    private final EmitInventoryEventWorker worker = new EmitInventoryEventWorker();

    @Test
    void taskDefName() {
        assertEquals("ch_emit_inventory_event", worker.getTaskDefName());
    }

    @Test
    void emitsInventoryReservedEvent() {
        Task task = taskWith(Map.of("eventType", "inventory.reserved", "orderId", "ORD-100"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("emitted"));
        assertEquals("inventory.reserved", result.getOutputData().get("eventType"));
    }

    @Test
    void outputContainsEmittedTrue() {
        Task task = taskWith(Map.of("eventType", "inventory.depleted", "orderId", "ORD-200"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("emitted"));
    }

    @Test
    void outputContainsEventType() {
        Task task = taskWith(Map.of("eventType", "inventory.released", "orderId", "ORD-300"));
        TaskResult result = worker.execute(task);

        assertEquals("inventory.released", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", null);
        input.put("orderId", "ORD-400");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("emitted"));
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesCustomEventType() {
        Task task = taskWith(Map.of("eventType", "inventory.low", "orderId", "ORD-500"));
        TaskResult result = worker.execute(task);

        assertEquals("inventory.low", result.getOutputData().get("eventType"));
        assertEquals(true, result.getOutputData().get("emitted"));
    }

    @Test
    void alwaysCompletes() {
        Task task = taskWith(Map.of("eventType", "inventory.reserved", "orderId", "ORD-600"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
