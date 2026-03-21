package llmcaching;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the LLM Caching workers.
 */
class LlmCachingTest {

    @BeforeEach
    void setUp() {
        // Clear the cache between tests to ensure deterministic behavior
        CacheLlmCallWorker.CACHE.clear();
    }

    @Test
    void testCacheHashPromptWorker() {
        CacheHashPromptWorker worker = new CacheHashPromptWorker();

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of(
                "prompt", "What is Conductor?",
                "model", "gpt-4"
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String cacheKey = (String) result.getOutputData().get("cacheKey");
        assertNotNull(cacheKey);
        assertEquals("gpt-4:What_is_Conductor?", cacheKey);
        assertTrue(cacheKey.length() <= 64);
    }

    @Test
    void testCacheHashPromptWorkerTruncation() {
        CacheHashPromptWorker worker = new CacheHashPromptWorker();

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of(
                "prompt", "This is a very long prompt that should be truncated because it exceeds sixty four characters when combined with the model name",
                "model", "gpt-4"
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String cacheKey = (String) result.getOutputData().get("cacheKey");
        assertNotNull(cacheKey);
        assertEquals(64, cacheKey.length());
    }

    @Test
    void testCacheLlmCallWorkerCacheMiss() {
        CacheLlmCallWorker worker = new CacheLlmCallWorker();

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of(
                "cacheKey", "gpt-4:What_is_Conductor?",
                "prompt", "What is Conductor?",
                "model", "gpt-4"
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("cacheHit"));
        assertEquals(850, result.getOutputData().get("latencyMs"));
        assertEquals(CacheLlmCallWorker.FIXED_RESPONSE, result.getOutputData().get("response"));
    }

    @Test
    void testCacheLlmCallWorkerCacheHit() {
        CacheLlmCallWorker worker = new CacheLlmCallWorker();

        // First call — cache miss
        Task task1 = new Task();
        task1.setStatus(Task.Status.IN_PROGRESS);
        task1.setInputData(new HashMap<>(Map.of(
                "cacheKey", "gpt-4:What_is_Conductor?",
                "prompt", "What is Conductor?",
                "model", "gpt-4"
        )));
        TaskResult result1 = worker.execute(task1);
        assertEquals(false, result1.getOutputData().get("cacheHit"));
        assertEquals(850, result1.getOutputData().get("latencyMs"));

        // Second call — cache hit
        Task task2 = new Task();
        task2.setStatus(Task.Status.IN_PROGRESS);
        task2.setInputData(new HashMap<>(Map.of(
                "cacheKey", "gpt-4:What_is_Conductor?",
                "prompt", "What is Conductor?",
                "model", "gpt-4"
        )));
        TaskResult result2 = worker.execute(task2);

        assertEquals(TaskResult.Status.COMPLETED, result2.getStatus());
        assertEquals(true, result2.getOutputData().get("cacheHit"));
        assertEquals(0, result2.getOutputData().get("latencyMs"));
        assertEquals(CacheLlmCallWorker.FIXED_RESPONSE, result2.getOutputData().get("response"));
    }

    @Test
    void testCacheReportWorkerCacheHit() {
        CacheReportWorker worker = new CacheReportWorker();

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of(
                "cacheHit", true,
                "response", "some response",
                "latencyMs", 0
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Yes \u2014 saved ~$0.02", result.getOutputData().get("saved"));
    }

    @Test
    void testCacheReportWorkerCacheMiss() {
        CacheReportWorker worker = new CacheReportWorker();

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of(
                "cacheHit", false,
                "response", CacheLlmCallWorker.FIXED_RESPONSE,
                "latencyMs", 850
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("No \u2014 first request", result.getOutputData().get("saved"));
    }

    @Test
    void testEndToEndCacheMissThenHit() {
        CacheHashPromptWorker hashWorker = new CacheHashPromptWorker();
        CacheLlmCallWorker llmWorker = new CacheLlmCallWorker();
        CacheReportWorker reportWorker = new CacheReportWorker();

        // Step 1: Hash the prompt
        Task hashTask = new Task();
        hashTask.setStatus(Task.Status.IN_PROGRESS);
        hashTask.setInputData(new HashMap<>(Map.of(
                "prompt", "What is Conductor?",
                "model", "gpt-4"
        )));
        TaskResult hashResult = hashWorker.execute(hashTask);
        String cacheKey = (String) hashResult.getOutputData().get("cacheKey");
        assertEquals("gpt-4:What_is_Conductor?", cacheKey);

        // Step 2: First LLM call — cache miss
        Task llmTask1 = new Task();
        llmTask1.setStatus(Task.Status.IN_PROGRESS);
        llmTask1.setInputData(new HashMap<>(Map.of(
                "cacheKey", cacheKey,
                "prompt", "What is Conductor?",
                "model", "gpt-4"
        )));
        TaskResult llmResult1 = llmWorker.execute(llmTask1);
        assertEquals(false, llmResult1.getOutputData().get("cacheHit"));
        assertEquals(850, llmResult1.getOutputData().get("latencyMs"));

        // Step 3: Report for cache miss
        Task reportTask1 = new Task();
        reportTask1.setStatus(Task.Status.IN_PROGRESS);
        reportTask1.setInputData(new HashMap<>(Map.of(
                "cacheHit", false,
                "response", llmResult1.getOutputData().get("response"),
                "latencyMs", 850
        )));
        TaskResult reportResult1 = reportWorker.execute(reportTask1);
        assertEquals("No \u2014 first request", reportResult1.getOutputData().get("saved"));

        // Step 4: Second LLM call — cache hit
        Task llmTask2 = new Task();
        llmTask2.setStatus(Task.Status.IN_PROGRESS);
        llmTask2.setInputData(new HashMap<>(Map.of(
                "cacheKey", cacheKey,
                "prompt", "What is Conductor?",
                "model", "gpt-4"
        )));
        TaskResult llmResult2 = llmWorker.execute(llmTask2);
        assertEquals(true, llmResult2.getOutputData().get("cacheHit"));
        assertEquals(0, llmResult2.getOutputData().get("latencyMs"));

        // Step 5: Report for cache hit
        Task reportTask2 = new Task();
        reportTask2.setStatus(Task.Status.IN_PROGRESS);
        reportTask2.setInputData(new HashMap<>(Map.of(
                "cacheHit", true,
                "response", llmResult2.getOutputData().get("response"),
                "latencyMs", 0
        )));
        TaskResult reportResult2 = reportWorker.execute(reportTask2);
        assertEquals("Yes \u2014 saved ~$0.02", reportResult2.getOutputData().get("saved"));
    }
}
