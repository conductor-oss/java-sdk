package llmretry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetryLlmCallWorkerTest {

    private final RetryLlmCallWorker worker = new RetryLlmCallWorker();

    @BeforeEach
    void setUp() {
        RetryLlmCallWorker.resetAttempts();
    }

    @Test
    void taskDefName() {
        assertEquals("retry_llm_call", worker.getTaskDefName());
    }

    @Test
    void firstAttemptReturnsFailed() {
        Task task = taskWith("wf-test-1", Map.of("prompt", "Explain retry patterns", "model", "gpt-4"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("429 Too Many Requests (attempt 1)", result.getReasonForIncompletion());
        assertEquals("rate_limited", result.getOutputData().get("error"));
        assertEquals(1, result.getOutputData().get("attempt"));
    }

    @Test
    void secondAttemptReturnsFailed() {
        Task task1 = taskWith("wf-test-2", Map.of("prompt", "test", "model", "gpt-4"));
        worker.execute(task1);

        Task task2 = taskWith("wf-test-2", Map.of("prompt", "test", "model", "gpt-4"));
        TaskResult result = worker.execute(task2);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("429 Too Many Requests (attempt 2)", result.getReasonForIncompletion());
        assertEquals(2, result.getOutputData().get("attempt"));
    }

    @Test
    void thirdAttemptReturnsCompleted() {
        String wfId = "wf-test-3";
        worker.execute(taskWith(wfId, Map.of("model", "gpt-4")));
        worker.execute(taskWith(wfId, Map.of("model", "gpt-4")));

        Task task3 = taskWith(wfId, Map.of("model", "gpt-4"));
        TaskResult result = worker.execute(task3);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Conductor handles retries so your AI pipeline never drops requests.",
                result.getOutputData().get("response"));
        assertEquals(3, result.getOutputData().get("attempts"));
        assertEquals("gpt-4", result.getOutputData().get("model"));
    }

    @Test
    void separateWorkflowsTrackIndependently() {
        worker.execute(taskWith("wf-a", Map.of("model", "gpt-4")));
        worker.execute(taskWith("wf-b", Map.of("model", "gpt-4")));

        TaskResult resultA = worker.execute(taskWith("wf-a", Map.of("model", "gpt-4")));
        TaskResult resultB = worker.execute(taskWith("wf-b", Map.of("model", "gpt-4")));

        assertEquals(TaskResult.Status.FAILED, resultA.getStatus());
        assertEquals(2, resultA.getOutputData().get("attempt"));

        assertEquals(TaskResult.Status.FAILED, resultB.getStatus());
        assertEquals(2, resultB.getOutputData().get("attempt"));
    }

    private Task taskWith(String workflowInstanceId, Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setWorkflowInstanceId(workflowInstanceId);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
