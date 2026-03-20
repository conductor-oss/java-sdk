package webhookretry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AttemptDeliveryWorkerTest {

    private final AttemptDeliveryWorker worker = new AttemptDeliveryWorker();

    @Test
    void taskDefName() {
        assertEquals("wr_attempt_delivery", worker.getTaskDefName());
    }

    @Test
    void firstAttemptReturns503() {
        Task task = taskWith(Map.of(
                "webhookUrl", "https://api.example.com/webhook",
                "payload", Map.of("event", "test"),
                "attempt", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(503, result.getOutputData().get("statusCode"));
        assertEquals(1, result.getOutputData().get("attempt"));
    }

    @Test
    void secondAttemptReturns503() {
        Task task = taskWith(Map.of(
                "webhookUrl", "https://api.example.com/webhook",
                "payload", Map.of("event", "test"),
                "attempt", 2));
        TaskResult result = worker.execute(task);

        assertEquals(503, result.getOutputData().get("statusCode"));
        assertEquals(2, result.getOutputData().get("attempt"));
    }

    @Test
    void thirdAttemptReturns200() {
        Task task = taskWith(Map.of(
                "webhookUrl", "https://api.example.com/webhook",
                "payload", Map.of("event", "test"),
                "attempt", 3));
        TaskResult result = worker.execute(task);

        assertEquals(200, result.getOutputData().get("statusCode"));
        assertEquals(3, result.getOutputData().get("attempt"));
    }

    @Test
    void backoffIncreasesWithAttempt() {
        Task task1 = taskWith(Map.of("attempt", 1));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("attempt", 2));
        TaskResult result2 = worker.execute(task2);

        assertEquals(1000, result1.getOutputData().get("backoffMs"));
        assertEquals(2000, result2.getOutputData().get("backoffMs"));
    }

    @Test
    void backoffIsZeroOnSuccess() {
        Task task = taskWith(Map.of("attempt", 3));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("backoffMs"));
    }

    @Test
    void handlesHighAttemptNumber() {
        Task task = taskWith(Map.of("attempt", 5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(200, result.getOutputData().get("statusCode"));
        assertEquals(0, result.getOutputData().get("backoffMs"));
    }

    @Test
    void handlesMissingAttemptInput() {
        Task task = taskWith(Map.of(
                "webhookUrl", "https://api.example.com/webhook",
                "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempt"));
        assertEquals(503, result.getOutputData().get("statusCode"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
