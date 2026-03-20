package selfcorrection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RunTestsWorkerTest {

    private final RunTestsWorker worker = new RunTestsWorker();

    @Test
    void taskDefName() {
        assertEquals("sc_run_tests", worker.getTaskDefName());
    }

    @Test
    void returnsFailTestResult() {
        Task task = taskWith(Map.of("code", "function fibonacci(n) { return n; }"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("fail", result.getOutputData().get("testResult"));
    }

    @Test
    void returnsErrors() {
        Task task = taskWith(Map.of("code", "function fibonacci(n) { return n; }"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertNotNull(errors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Maximum call stack exceeded"));
    }

    @Test
    void returnsTestCounts() {
        Task task = taskWith(Map.of("code", "function fibonacci(n) { return n; }"));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("testsPassed"));
        assertEquals(1, result.getOutputData().get("testsFailed"));
    }

    @Test
    void errorsReferenceNegativeInput() {
        Task task = taskWith(Map.of("code", "function fibonacci(n) { return n; }"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertTrue(errors.get(0).contains("negative input"));
    }

    @Test
    void handlesEmptyCode() {
        Map<String, Object> input = new HashMap<>();
        input.put("code", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("fail", result.getOutputData().get("testResult"));
    }

    @Test
    void handlesNullCode() {
        Map<String, Object> input = new HashMap<>();
        input.put("code", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("errors"));
    }

    @Test
    void handlesMissingCode() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("fail", result.getOutputData().get("testResult"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
