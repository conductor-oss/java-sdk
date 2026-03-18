package anthropicclaude.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeCallApiWorkerTest {

    @Test
    void taskDefName() {
        ClaudeCallApiWorker worker = new ClaudeCallApiWorker("sk-test-key");
        assertEquals("claude_call_api", worker.getTaskDefName());
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("CONDUCTOR_ANTHROPIC_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, ClaudeCallApiWorker::new);
        }
    }

    @Test
    void non200ResponsesExposeActionableFailureDetails() {
        TaskResult failed = new ClaudeCallApiWorker("sk-test-key")
                .handleHttpFailure(new TaskResult(taskWith(new HashMap<>())), 400,
                        "{\"error\":{\"message\":\"model claude-sonnet-4-20250514 is not available\"}}");

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, failed.getStatus());
        assertTrue(failed.getReasonForIncompletion().contains("HTTP 400"));
        assertTrue(failed.getReasonForIncompletion().contains("not available"));
        assertEquals(400, failed.getOutputData().get("httpStatus"));
        assertTrue(((String) failed.getOutputData().get("errorBody")).contains("model claude-sonnet-4-20250514"));
    }

    @Test
    void rateLimitResponseIsRetryable() {
        TaskResult failed = new ClaudeCallApiWorker("sk-test-key")
                .handleHttpFailure(new TaskResult(taskWith(new HashMap<>())), 429,
                        "{\"error\":{\"message\":\"rate limited\"}}");

        assertEquals(TaskResult.Status.FAILED, failed.getStatus());
        assertTrue(failed.getReasonForIncompletion().contains("HTTP 429"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
