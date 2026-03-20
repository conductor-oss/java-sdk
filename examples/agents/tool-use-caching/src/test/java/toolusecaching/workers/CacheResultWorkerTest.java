package toolusecaching.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheResultWorkerTest {

    private final CacheResultWorker worker = new CacheResultWorker();

    @Test
    void taskDefName() {
        assertEquals("uc_cache_result", worker.getTaskDefName());
    }

    @Test
    void cachesResultSuccessfully() {
        Map<String, Object> toolResult = Map.of("rate", 0.92, "convertedAmount", 920.0);
        Task task = taskWith(Map.of(
                "cacheKey", "currency_exchange:{from=USD, to=EUR, amount=1000}",
                "result", toolResult,
                "ttlSeconds", 300));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(toolResult, result.getOutputData().get("storedResult"));
        assertEquals(true, result.getOutputData().get("cached"));
        assertEquals("currency_exchange:{from=USD, to=EUR, amount=1000}", result.getOutputData().get("cacheKey"));
        assertEquals("2026-03-08T10:05:00Z", result.getOutputData().get("expiresAt"));
        assertEquals(300, result.getOutputData().get("ttlSeconds"));
    }

    @Test
    void returnsFixedExpiresAt() {
        Task task = taskWith(Map.of(
                "cacheKey", "some_key",
                "result", "some_value",
                "ttlSeconds", 60));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-08T10:05:00Z", result.getOutputData().get("expiresAt"));
    }

    @Test
    void returnsCachedTrue() {
        Task task = taskWith(Map.of(
                "cacheKey", "key1",
                "result", Map.of("data", "test"),
                "ttlSeconds", 300));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("cached"));
    }

    @Test
    void preservesTtlSeconds() {
        Task task = taskWith(Map.of(
                "cacheKey", "key1",
                "result", "value",
                "ttlSeconds", 600));
        TaskResult result = worker.execute(task);

        assertEquals(600, result.getOutputData().get("ttlSeconds"));
    }

    @Test
    void handlesNullCacheKey() {
        Map<String, Object> input = new HashMap<>();
        input.put("cacheKey", null);
        input.put("result", "value");
        input.put("ttlSeconds", 300);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("cacheKey"));
    }

    @Test
    void handlesNullResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("cacheKey", "key1");
        input.put("result", null);
        input.put("ttlSeconds", 300);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNull(result.getOutputData().get("storedResult"));
        assertEquals(true, result.getOutputData().get("cached"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("cached"));
        assertEquals("unknown", result.getOutputData().get("cacheKey"));
        assertEquals(300, result.getOutputData().get("ttlSeconds"));
    }

    @Test
    void handlesBlankCacheKey() {
        Task task = taskWith(Map.of(
                "cacheKey", "  ",
                "result", "value",
                "ttlSeconds", 120));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("cacheKey"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
