package idempotentstart.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IdemProcessWorkerTest {

    private final IdemProcessWorker worker = new IdemProcessWorker();

    @Test
    void taskDefName() {
        assertEquals("idem_process", worker.getTaskDefName());
    }

    @Test
    void processesNumericOrderId() {
        Task task = taskWith(Map.of("orderId", 12345, "amount", 99.95));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order-12345-done", result.getOutputData().get("result"));
        assertEquals("12345", result.getOutputData().get("orderId"));
    }

    @Test
    void processesStringOrderId() {
        Task task = taskWith(Map.of("orderId", "ABC-001", "amount", 50));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order-ABC-001-done", result.getOutputData().get("result"));
        assertEquals("ABC-001", result.getOutputData().get("orderId"));
    }

    @Test
    void defaultsOrderIdWhenMissing() {
        Task task = taskWith(Map.of("amount", 10));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order-unknown-done", result.getOutputData().get("result"));
        assertEquals("unknown", result.getOutputData().get("orderId"));
    }

    @Test
    void defaultsOrderIdWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", null);
        input.put("amount", 10);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order-unknown-done", result.getOutputData().get("result"));
        assertEquals("unknown", result.getOutputData().get("orderId"));
    }

    @Test
    void defaultsOrderIdWhenBlank() {
        Task task = taskWith(Map.of("orderId", "   ", "amount", 10));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order-unknown-done", result.getOutputData().get("result"));
        assertEquals("unknown", result.getOutputData().get("orderId"));
    }

    @Test
    void outputContainsResultKey() {
        Task task = taskWith(Map.of("orderId", 1, "amount", 5));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void outputContainsOrderIdKey() {
        Task task = taskWith(Map.of("orderId", "X99", "amount", 100));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("orderId"));
        assertEquals("X99", result.getOutputData().get("orderId"));
    }

    @Test
    void resultFormatIncludesOrderId() {
        Task task = taskWith(Map.of("orderId", "abc", "amount", 42));
        TaskResult result = worker.execute(task);

        String output = (String) result.getOutputData().get("result");
        assertTrue(output.startsWith("order-"));
        assertTrue(output.endsWith("-done"));
        assertTrue(output.contains("abc"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
