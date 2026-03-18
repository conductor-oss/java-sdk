package googlegemini.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeminiGenerateWorkerTest {

    @Test
    void taskDefName() {
        GeminiGenerateWorker worker = new GeminiGenerateWorker("google-test-key");
        assertEquals("gemini_generate", worker.getTaskDefName());
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("GOOGLE_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, GeminiGenerateWorker::new);
        }
    }

    @Test
    void constructorThrowsWithBlankKey() {
        String key = System.getenv("GOOGLE_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, GeminiGenerateWorker::new);
        }
    }

    @Test
    void quotaFailuresIncludeRetryDetailsAndFailFast() {
        TaskResult failed = new GeminiGenerateWorker("google-test-key")
                .handleHttpFailure(new TaskResult(taskWith(new HashMap<>())), "gemini-2.5-flash", 429,
                        "{\"error\":{\"message\":\"RESOURCE_EXHAUSTED\"}}", "120");

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, failed.getStatus());
        assertTrue(failed.getReasonForIncompletion().contains("quota exceeded"));
        assertTrue(failed.getReasonForIncompletion().contains("120"));
        assertEquals("120", failed.getOutputData().get("retryAfterSeconds"));
        assertEquals("gemini-2.5-flash", failed.getOutputData().get("model"));
    }

    @Test
    void transientErrorIsRetryable() {
        TaskResult failed = new GeminiGenerateWorker("google-test-key")
                .handleHttpFailure(new TaskResult(taskWith(new HashMap<>())), "gemini-2.5-flash", 503,
                        "{\"error\":{\"message\":\"Service unavailable\"}}", null);

        assertEquals(TaskResult.Status.FAILED, failed.getStatus());
        assertTrue(failed.getReasonForIncompletion().contains("transient error"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
