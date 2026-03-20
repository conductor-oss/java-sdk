package taskdedup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReturnCachedWorkerTest {

    private final ReturnCachedWorker worker = new ReturnCachedWorker();

    @Test
    void taskDefName() {
        assertEquals("tdd_return_cached", worker.getTaskDefName());
    }

    @Test
    void returnsCachedResult() {
        Map<String, Object> cached = Map.of("cached", true, "value", "previous_result");
        Task task = taskWith(Map.of("hash", "sha256:abc123", "cachedResult", cached));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(cached, result.getOutputData().get("result"));
    }

    @Test
    void marksFromCache() {
        Task task = taskWith(Map.of("hash", "sha256:def456", "cachedResult", "old-data"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("fromCache"));
    }

    @Test
    void handlesNullCachedResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("hash", "sha256:ghi789");
        input.put("cachedResult", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNull(result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("fromCache"));
    }

    @Test
    void handlesNullHash() {
        Map<String, Object> input = new HashMap<>();
        input.put("hash", null);
        input.put("cachedResult", "data");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("fromCache"));
    }

    @Test
    void outputContainsBothKeys() {
        Task task = taskWith(Map.of("hash", "sha256:test", "cachedResult", "val"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("fromCache"));
    }

    @Test
    void handlesMapCachedResult() {
        Map<String, Object> cachedData = Map.of("status", "done", "count", 42);
        Task task = taskWith(Map.of("hash", "sha256:map001", "cachedResult", cachedData));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(cachedData, result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
