package toolusecaching.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckCacheWorkerTest {

    private final CheckCacheWorker worker = new CheckCacheWorker();

    @Test
    void taskDefName() {
        assertEquals("uc_check_cache", worker.getTaskDefName());
    }

    @Test
    void returnsMissForCurrencyExchange() {
        Task task = taskWith(Map.of(
                "toolName", "currency_exchange",
                "toolArgs", Map.of("from", "USD", "to", "EUR", "amount", 1000),
                "cacheTtlSeconds", 300));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("miss", result.getOutputData().get("cacheStatus"));
        assertNotNull(result.getOutputData().get("cacheKey"));
        assertNull(result.getOutputData().get("cachedResult"));
        assertNull(result.getOutputData().get("cachedAt"));
        assertEquals("No cache entry exists for this key", result.getOutputData().get("reason"));
    }

    @Test
    void cacheKeyContainsToolName() {
        Task task = taskWith(Map.of(
                "toolName", "weather_lookup",
                "toolArgs", Map.of("city", "London"),
                "cacheTtlSeconds", 60));
        TaskResult result = worker.execute(task);

        String cacheKey = (String) result.getOutputData().get("cacheKey");
        assertTrue(cacheKey.contains("weather_lookup"));
    }

    @Test
    void cacheKeyContainsToolArgs() {
        Task task = taskWith(Map.of(
                "toolName", "currency_exchange",
                "toolArgs", Map.of("from", "USD", "to", "EUR"),
                "cacheTtlSeconds", 300));
        TaskResult result = worker.execute(task);

        String cacheKey = (String) result.getOutputData().get("cacheKey");
        assertTrue(cacheKey.contains("USD"));
        assertTrue(cacheKey.contains("EUR"));
    }

    @Test
    void handlesNullToolName() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", null);
        input.put("toolArgs", Map.of("key", "value"));
        input.put("cacheTtlSeconds", 300);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("miss", result.getOutputData().get("cacheStatus"));
        String cacheKey = (String) result.getOutputData().get("cacheKey");
        assertTrue(cacheKey.contains("unknown_tool"));
    }

    @Test
    void handlesNullToolArgs() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", "some_tool");
        input.put("toolArgs", null);
        input.put("cacheTtlSeconds", 300);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("cacheKey"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("miss", result.getOutputData().get("cacheStatus"));
    }

    @Test
    void handlesBlankToolName() {
        Task task = taskWith(Map.of(
                "toolName", "  ",
                "toolArgs", Map.of(),
                "cacheTtlSeconds", 300));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String cacheKey = (String) result.getOutputData().get("cacheKey");
        assertTrue(cacheKey.contains("unknown_tool"));
    }

    @Test
    void handlesMissingCacheTtl() {
        Task task = taskWith(Map.of(
                "toolName", "some_tool",
                "toolArgs", Map.of("a", 1)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("miss", result.getOutputData().get("cacheStatus"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
