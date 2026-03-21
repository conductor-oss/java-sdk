package webhookratelimiting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessAllowedWorkerTest {

    private final ProcessAllowedWorker worker = new ProcessAllowedWorker();

    @Test
    void taskDefName() {
        assertEquals("wl_process_allowed", worker.getTaskDefName());
    }

    @Test
    void processesWebhookSuccessfully() {
        Task task = taskWith(Map.of(
                "senderId", "partner-api-xyz",
                "payload", Map.of("event", "data.sync", "records", 150)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("partner-api-xyz", result.getOutputData().get("senderId"));
    }

    @Test
    void outputContainsProcessedFlag() {
        Task task = taskWith(Map.of("senderId", "sender-1", "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void preservesSenderIdInOutput() {
        Task task = taskWith(Map.of("senderId", "webhook-client-99", "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("webhook-client-99", result.getOutputData().get("senderId"));
    }

    @Test
    void handlesNullSenderId() {
        Map<String, Object> input = new HashMap<>();
        input.put("senderId", null);
        input.put("payload", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("senderId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("senderId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesEmptyStringSenderId() {
        Task task = taskWith(Map.of("senderId", "", "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("senderId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesDifferentPayloadTypes() {
        Task task = taskWith(Map.of(
                "senderId", "sender-1",
                "payload", "raw-string-payload"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
