package toolratelimit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckRateLimitWorkerTest {

    private final CheckRateLimitWorker worker = new CheckRateLimitWorker();

    @Test
    void taskDefName() {
        assertEquals("rl_check_rate_limit", worker.getTaskDefName());
    }

    @Test
    void returnsThrottledDecision() {
        Task task = taskWith(Map.of("toolName", "translation_api", "apiKey", "key-prod-abc123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("throttled", result.getOutputData().get("decision"));
        assertEquals(98, result.getOutputData().get("quotaUsed"));
        assertEquals(100, result.getOutputData().get("quotaLimit"));
        assertEquals(2, result.getOutputData().get("quotaRemaining"));
    }

    @Test
    void returnsRetryAfterMs() {
        Task task = taskWith(Map.of("toolName", "translation_api", "apiKey", "key-prod-abc123"));
        TaskResult result = worker.execute(task);

        assertEquals(5000, result.getOutputData().get("retryAfterMs"));
        assertEquals(3, result.getOutputData().get("queuePosition"));
    }

    @Test
    void returnsFixedWindowResetAt() {
        Task task = taskWith(Map.of("toolName", "translation_api", "apiKey", "key-prod-abc123"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-08T10:01:00Z", result.getOutputData().get("windowResetAt"));
    }

    @Test
    void handlesNullToolName() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", null);
        input.put("apiKey", "key-prod-abc123");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("throttled", result.getOutputData().get("decision"));
    }

    @Test
    void handlesBlankApiKey() {
        Task task = taskWith(Map.of("toolName", "translation_api", "apiKey", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("decision"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("throttled", result.getOutputData().get("decision"));
        assertEquals(2, result.getOutputData().get("quotaRemaining"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("toolName", "translation_api", "apiKey", "key-prod-abc123"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("quotaUsed"));
        assertNotNull(result.getOutputData().get("quotaLimit"));
        assertNotNull(result.getOutputData().get("quotaRemaining"));
        assertNotNull(result.getOutputData().get("decision"));
        assertNotNull(result.getOutputData().get("retryAfterMs"));
        assertNotNull(result.getOutputData().get("queuePosition"));
        assertNotNull(result.getOutputData().get("windowResetAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
