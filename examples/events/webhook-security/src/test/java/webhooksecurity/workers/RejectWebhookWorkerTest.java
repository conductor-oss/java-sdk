package webhooksecurity.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RejectWebhookWorkerTest {

    private final RejectWebhookWorker worker = new RejectWebhookWorker();

    @Test
    void taskDefName() {
        assertEquals("ws_reject_webhook", worker.getTaskDefName());
    }

    @Test
    void rejectsWithReasonAndProvided() {
        Task task = taskWith(Map.of("reason", "Signature mismatch", "provided", "bad_signature"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
        assertEquals("Signature mismatch", result.getOutputData().get("reason"));
    }

    @Test
    void rejectsWithCustomReason() {
        Task task = taskWith(Map.of("reason", "Expired token", "provided", "expired_sig"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
        assertEquals("Expired token", result.getOutputData().get("reason"));
    }

    @Test
    void rejectsWithEmptyReason() {
        Task task = taskWith(Map.of("reason", "", "provided", "some_sig"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
        assertEquals("", result.getOutputData().get("reason"));
    }

    @Test
    void handlesNullReason() {
        Map<String, Object> input = new HashMap<>();
        input.put("reason", null);
        input.put("provided", "bad_sig");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
        assertEquals("unknown", result.getOutputData().get("reason"));
    }

    @Test
    void handlesNullProvided() {
        Map<String, Object> input = new HashMap<>();
        input.put("reason", "Signature mismatch");
        input.put("provided", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
        assertEquals("Signature mismatch", result.getOutputData().get("reason"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
        assertEquals("unknown", result.getOutputData().get("reason"));
    }

    @Test
    void outputContainsRejectedAndReason() {
        Task task = taskWith(Map.of("reason", "Invalid", "provided", "xyz"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("rejected"));
        assertTrue(result.getOutputData().containsKey("reason"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
