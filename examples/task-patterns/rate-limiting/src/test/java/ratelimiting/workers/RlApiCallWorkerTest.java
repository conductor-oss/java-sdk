package ratelimiting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RlApiCallWorkerTest {

    private final RlApiCallWorker worker = new RlApiCallWorker();

    @Test
    void taskDefName() {
        assertEquals("rl_api_call", worker.getTaskDefName());
    }

    @Test
    void processesNumericBatchId() {
        Task task = taskWith(Map.of("batchId", 42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("batch-42-done", result.getOutputData().get("result"));
    }

    @Test
    void processesStringBatchId() {
        Task task = taskWith(Map.of("batchId", "7"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("batch-7-done", result.getOutputData().get("result"));
    }

    @Test
    void defaultsBatchIdWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("batch-unknown-done", result.getOutputData().get("result"));
    }

    @Test
    void defaultsBatchIdWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("batchId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("batch-unknown-done", result.getOutputData().get("result"));
    }

    @Test
    void defaultsBatchIdWhenBlank() {
        Task task = taskWith(Map.of("batchId", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("batch-unknown-done", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResultKey() {
        Task task = taskWith(Map.of("batchId", 1));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void resultFormatIncludesBatchId() {
        Task task = taskWith(Map.of("batchId", "abc"));
        TaskResult result = worker.execute(task);

        String output = (String) result.getOutputData().get("result");
        assertTrue(output.startsWith("batch-"));
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
