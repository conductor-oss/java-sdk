package webhooksecurity.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessWebhookWorkerTest {

    private final ProcessWebhookWorker worker = new ProcessWebhookWorker();

    @Test
    void taskDefName() {
        assertEquals("ws_process_webhook", worker.getTaskDefName());
    }

    @Test
    void processesWebhookWithPayload() {
        Task task = taskWith(Map.of("payload", "{\"event\":\"push\",\"repo\":\"my-repo\"}"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void processesWebhookWithEmptyPayload() {
        Task task = taskWith(Map.of("payload", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("payload", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void processesLargePayload() {
        String largePayload = "{\"data\":\"" + "x".repeat(5000) + "\"}";
        Task task = taskWith(Map.of("payload", largePayload));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void processesJsonPayload() {
        Task task = taskWith(Map.of("payload", "{\"action\":\"created\",\"sender\":\"user1\"}"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsOnlyProcessedField() {
        Task task = taskWith(Map.of("payload", "test"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
