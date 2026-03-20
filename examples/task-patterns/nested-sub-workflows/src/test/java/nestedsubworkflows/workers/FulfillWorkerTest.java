package nestedsubworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FulfillWorkerTest {

    private final FulfillWorker worker = new FulfillWorker();

    @Test
    void taskDefName() {
        assertEquals("nest_fulfill", worker.getTaskDefName());
    }

    @Test
    void fulfillsOrderSuccessfully() {
        Task task = taskWith(Map.of("orderId", "ORD-5001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("fulfilled"));
    }

    @Test
    void handlesCustomOrderId() {
        Task task = taskWith(Map.of("orderId", "ORD-999"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("fulfilled"));
    }

    @Test
    void defaultsOrderIdWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("fulfilled"));
    }

    @Test
    void defaultsOrderIdWhenBlank() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "   ");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("fulfilled"));
    }

    @Test
    void defaultsOrderIdWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("fulfilled"));
    }

    @Test
    void outputContainsExpectedFields() {
        Task task = taskWith(Map.of("orderId", "ORD-001"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("fulfilled"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
