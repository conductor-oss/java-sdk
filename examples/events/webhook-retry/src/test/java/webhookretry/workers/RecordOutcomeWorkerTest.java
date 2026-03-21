package webhookretry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordOutcomeWorkerTest {

    private final RecordOutcomeWorker worker = new RecordOutcomeWorker();

    @Test
    void taskDefName() {
        assertEquals("wr_record_outcome", worker.getTaskDefName());
    }

    @Test
    void recordsDeliveryOutcome() {
        Task task = taskWith(Map.of(
                "totalAttempts", 3,
                "webhookUrl", "https://api.example.com/webhook"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("delivered after retries", result.getOutputData().get("outcome"));
        assertEquals(3, result.getOutputData().get("totalAttempts"));
    }

    @Test
    void outputContainsTotalAttempts() {
        Task task = taskWith(Map.of(
                "totalAttempts", 5,
                "webhookUrl", "https://hooks.example.com/notify"));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("totalAttempts"));
    }

    @Test
    void outcomeIsAlwaysDeliveredAfterRetries() {
        Task task = taskWith(Map.of(
                "totalAttempts", 1,
                "webhookUrl", "https://api.example.com/webhook"));
        TaskResult result = worker.execute(task);

        assertEquals("delivered after retries", result.getOutputData().get("outcome"));
    }

    @Test
    void handlesNullWebhookUrl() {
        Map<String, Object> input = new HashMap<>();
        input.put("totalAttempts", 2);
        input.put("webhookUrl", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("delivered after retries", result.getOutputData().get("outcome"));
    }

    @Test
    void handlesNullTotalAttempts() {
        Map<String, Object> input = new HashMap<>();
        input.put("totalAttempts", null);
        input.put("webhookUrl", "https://api.example.com/webhook");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("totalAttempts"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("delivered after retries", result.getOutputData().get("outcome"));
        assertEquals(1, result.getOutputData().get("totalAttempts"));
    }

    @Test
    void handlesDifferentAttemptCounts() {
        Task task = taskWith(Map.of("totalAttempts", 10, "webhookUrl", "https://example.com"));
        TaskResult result = worker.execute(task);

        assertEquals(10, result.getOutputData().get("totalAttempts"));
        assertEquals("delivered after retries", result.getOutputData().get("outcome"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
