package selfcorrection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FixWorkerTest {

    private final FixWorker worker = new FixWorker();

    @Test
    void taskDefName() {
        assertEquals("sc_fix", worker.getTaskDefName());
    }

    @Test
    void returnsFixedCode() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { if (n <= 1) return n; return fibonacci(n-1) + fibonacci(n-2); }",
                "diagnosis", "Missing guard for negative numbers"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("fixedCode"));
    }

    @Test
    void fixedCodeContainsNegativeGuard() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { if (n <= 1) return n; return fibonacci(n-1) + fibonacci(n-2); }",
                "diagnosis", "Missing guard for negative numbers"));
        TaskResult result = worker.execute(task);

        String fixedCode = (String) result.getOutputData().get("fixedCode");
        assertTrue(fixedCode.contains("n < 0"));
    }

    @Test
    void fixedCodeThrowsOnNegative() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { if (n <= 1) return n; return fibonacci(n-1) + fibonacci(n-2); }",
                "diagnosis", "Missing guard for negative numbers"));
        TaskResult result = worker.execute(task);

        String fixedCode = (String) result.getOutputData().get("fixedCode");
        assertTrue(fixedCode.contains("throw new Error"));
        assertTrue(fixedCode.contains("Negative input"));
    }

    @Test
    void returnsChangesMade() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { if (n <= 1) return n; return fibonacci(n-1) + fibonacci(n-2); }",
                "diagnosis", "Missing guard for negative numbers"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> changes = (List<String>) result.getOutputData().get("changesMade");
        assertNotNull(changes);
        assertFalse(changes.isEmpty());
        assertTrue(changes.get(0).contains("negative input guard"));
    }

    @Test
    void fixedCodeStillContainsFibonacciLogic() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { if (n <= 1) return n; return fibonacci(n-1) + fibonacci(n-2); }",
                "diagnosis", "Missing guard for negative numbers"));
        TaskResult result = worker.execute(task);

        String fixedCode = (String) result.getOutputData().get("fixedCode");
        assertTrue(fixedCode.contains("fibonacci(n-1)"));
        assertTrue(fixedCode.contains("fibonacci(n-2)"));
    }

    @Test
    void handlesEmptyCode() {
        Map<String, Object> input = new HashMap<>();
        input.put("code", "");
        input.put("diagnosis", "Some diagnosis");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("fixedCode"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("fixedCode"));
        assertNotNull(result.getOutputData().get("changesMade"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
