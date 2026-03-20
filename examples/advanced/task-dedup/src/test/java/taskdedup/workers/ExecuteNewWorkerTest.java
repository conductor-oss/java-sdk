package taskdedup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteNewWorkerTest {

    private final ExecuteNewWorker worker = new ExecuteNewWorker();

    @Test
    void taskDefName() {
        assertEquals("tdd_execute_new", worker.getTaskDefName());
    }

    @Test
    void processesNewTaskSuccessfully() {
        Task task = taskWith(Map.of("payload", Map.of("orderId", "ORD-123"), "hash", "sha256:abc123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed_successfully", result.getOutputData().get("result"));
    }

    @Test
    void marksCachedForFuture() {
        Task task = taskWith(Map.of("payload", "data", "hash", "sha256:def456"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("cachedForFuture"));
    }

    @Test
    void outputContainsHash() {
        Task task = taskWith(Map.of("payload", "data", "hash", "sha256:ghi789"));
        TaskResult result = worker.execute(task);

        assertEquals("sha256:ghi789", result.getOutputData().get("hash"));
    }

    @Test
    void handlesNullHash() {
        Map<String, Object> input = new HashMap<>();
        input.put("payload", "data");
        input.put("hash", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("hash"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed_successfully", result.getOutputData().get("result"));
    }

    @Test
    void outputHasAllExpectedKeys() {
        Task task = taskWith(Map.of("payload", "test", "hash", "sha256:test000"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("cachedForFuture"));
        assertTrue(result.getOutputData().containsKey("hash"));
    }

    @Test
    void handlesComplexPayload() {
        Task task = taskWith(Map.of(
                "payload", Map.of("orderId", "ORD-999", "items", 5, "total", 250.00),
                "hash", "sha256:complex123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed_successfully", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
