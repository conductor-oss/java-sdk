package idempotentprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SkipWorkerTest {

    private final SkipWorker worker = new SkipWorker();

    @Test
    void skipReturnsProperReason() {
        Task task = taskWithInput(Map.of(
                "messageId", "msg-dup",
                "previousResult", "sha256:abc123"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("skipped"));

        String reason = (String) result.getOutputData().get("reason");
        assertNotNull(reason);
        assertTrue(reason.contains("Duplicate"),
                "Reason should mention duplicate");
        assertTrue(reason.contains("sha256:abc123"),
                "Reason should include the previous result hash");
    }

    @Test
    void outputIncludesMessageId() {
        Task task = taskWithInput(Map.of(
                "messageId", "msg-skip-test",
                "previousResult", "sha256:fff"));

        TaskResult result = worker.execute(task);

        assertEquals("msg-skip-test", result.getOutputData().get("messageId"));
    }

    @Test
    void outputIncludesPreviousResult() {
        Task task = taskWithInput(Map.of(
                "messageId", "msg-skip-test",
                "previousResult", "sha256:deadbeef"));

        TaskResult result = worker.execute(task);

        assertEquals("sha256:deadbeef", result.getOutputData().get("previousResult"));
    }

    @Test
    void taskDefNameIsCorrect() {
        assertEquals("idp_skip", worker.getTaskDefName());
    }

    private static Task taskWithInput(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setTaskId("test-task-id");
        task.setReferenceTaskName("idp_skip_ref");
        task.setWorkflowInstanceId("test-workflow-id");
        task.setStatus(Task.Status.IN_PROGRESS);
        task.getInputData().putAll(input);
        return task;
    }
}
