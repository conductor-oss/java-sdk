package eventdrivenworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HandleOrderWorkerTest {

    private final HandleOrderWorker worker = new HandleOrderWorker();

    @Test
    void taskDefName() {
        assertEquals("ed_handle_order", worker.getTaskDefName());
    }

    @Test
    void handlesOrderWithOrderId() {
        Task task = taskWith(Map.of(
                "eventData", Map.of("orderId", "ORD-9921", "amount", 340.50),
                "priority", "normal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order", result.getOutputData().get("handler"));
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("ORD-9921", result.getOutputData().get("orderId"));
    }

    @Test
    void handlesHighPriorityOrder() {
        Task task = taskWith(Map.of(
                "eventData", Map.of("orderId", "ORD-1234"),
                "priority", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order", result.getOutputData().get("handler"));
        assertEquals("ORD-1234", result.getOutputData().get("orderId"));
    }

    @Test
    void outputAlwaysHasHandlerOrder() {
        Task task = taskWith(Map.of(
                "eventData", Map.of("orderId", "ORD-555"),
                "priority", "normal"));
        TaskResult result = worker.execute(task);

        assertEquals("order", result.getOutputData().get("handler"));
    }

    @Test
    void outputAlwaysHasProcessedTrue() {
        Task task = taskWith(Map.of(
                "eventData", Map.of("orderId", "ORD-555"),
                "priority", "normal"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesEmptyEventData() {
        Task task = taskWith(Map.of(
                "eventData", Map.of(),
                "priority", "normal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("orderId"));
    }

    @Test
    void handlesNullPriority() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventData", Map.of("orderId", "ORD-814"));
        input.put("priority", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-814", result.getOutputData().get("orderId"));
    }

    @Test
    void handlesNullEventData() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventData", null);
        input.put("priority", "normal");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("orderId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order", result.getOutputData().get("handler"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
