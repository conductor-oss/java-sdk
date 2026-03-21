package selfcorrection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeliverWorkerTest {

    private final DeliverWorker worker = new DeliverWorker();

    @Test
    void taskDefName() {
        assertEquals("sc_deliver", worker.getTaskDefName());
    }

    @Test
    void deliversCode() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { return n; }",
                "testsPassed", 5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("function fibonacci(n) { return n; }", result.getOutputData().get("deliveredCode"));
    }

    @Test
    void returnsSuccessStatus() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { return n; }",
                "testsPassed", 5));
        TaskResult result = worker.execute(task);

        assertEquals("success", result.getOutputData().get("deliveryStatus"));
    }

    @Test
    void handlesWasFixedTrue() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { return n; }",
                "testsPassed", 3,
                "wasFixed", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("deliveryStatus"));
    }

    @Test
    void handlesWasFixedFalse() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { return n; }",
                "testsPassed", 5,
                "wasFixed", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("deliveryStatus"));
    }

    @Test
    void handlesZeroTestsPassed() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { return n; }",
                "testsPassed", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("function fibonacci(n) { return n; }", result.getOutputData().get("deliveredCode"));
    }

    @Test
    void handlesEmptyCode() {
        Map<String, Object> input = new HashMap<>();
        input.put("code", "");
        input.put("testsPassed", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("deliveredCode"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("deliveredCode"));
        assertEquals("success", result.getOutputData().get("deliveryStatus"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
