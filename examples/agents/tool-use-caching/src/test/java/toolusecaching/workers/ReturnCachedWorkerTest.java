package toolusecaching.workers;

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
        assertEquals("uc_return_cached", worker.getTaskDefName());
    }

    @Test
    void returnsCachedResultSuccessfully() {
        Map<String, Object> cachedResult = Map.of("rate", 0.92, "amount", 1000);
        Task task = taskWith(Map.of(
                "cachedResult", cachedResult,
                "cacheKey", "currency_exchange:{from=USD, to=EUR}",
                "cachedAt", "2026-03-08T09:55:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(cachedResult, result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("fromCache"));
        assertEquals("45s", result.getOutputData().get("cacheAge"));
    }

    @Test
    void setsFromCacheTrue() {
        Task task = taskWith(Map.of(
                "cachedResult", "some_value",
                "cacheKey", "key1",
                "cachedAt", "2026-03-08T09:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("fromCache"));
    }

    @Test
    void returnsCacheAgeAs45s() {
        Task task = taskWith(Map.of(
                "cachedResult", Map.of("data", "test"),
                "cacheKey", "test_key",
                "cachedAt", "2026-03-08T09:59:15Z"));
        TaskResult result = worker.execute(task);

        assertEquals("45s", result.getOutputData().get("cacheAge"));
    }

    @Test
    void handlesNullCachedResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("cachedResult", null);
        input.put("cacheKey", "key1");
        input.put("cachedAt", "2026-03-08T09:55:00Z");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNull(result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("fromCache"));
    }

    @Test
    void handlesNullCacheKey() {
        Map<String, Object> input = new HashMap<>();
        input.put("cachedResult", "data");
        input.put("cacheKey", null);
        input.put("cachedAt", "2026-03-08T09:55:00Z");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesNullCachedAt() {
        Map<String, Object> input = new HashMap<>();
        input.put("cachedResult", "data");
        input.put("cacheKey", "key1");
        input.put("cachedAt", null);
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
        assertEquals("45s", result.getOutputData().get("cacheAge"));
    }

    @Test
    void preservesComplexCachedResult() {
        Map<String, Object> complexResult = Map.of(
                "baseCurrency", "USD",
                "targetCurrency", "EUR",
                "rate", 0.92,
                "convertedAmount", 920.0);
        Task task = taskWith(Map.of(
                "cachedResult", complexResult,
                "cacheKey", "currency_exchange:{from=USD, to=EUR, amount=1000}",
                "cachedAt", "2026-03-08T09:55:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(complexResult, result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
