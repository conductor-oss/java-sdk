package toolratelimit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueueRequestWorkerTest {

    private final QueueRequestWorker worker = new QueueRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("rl_queue_request", worker.getTaskDefName());
    }

    @Test
    void queuesRequestSuccessfully() {
        Task task = taskWith(Map.of(
                "toolName", "translation_api",
                "toolArgs", Map.of("text", "Hello"),
                "retryAfterMs", 5000,
                "queuePosition", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("q-fixed-001", result.getOutputData().get("queueId"));
        assertEquals(3, result.getOutputData().get("queuePosition"));
        assertEquals(5000, result.getOutputData().get("estimatedWaitMs"));
        assertEquals("queued", result.getOutputData().get("status"));
    }

    @Test
    void returnsFixedQueuedAt() {
        Task task = taskWith(Map.of(
                "toolName", "translation_api",
                "retryAfterMs", 5000,
                "queuePosition", 3));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-08T10:00:00Z", result.getOutputData().get("queuedAt"));
    }

    @Test
    void usesRetryAfterMsAsEstimatedWait() {
        Task task = taskWith(Map.of(
                "toolName", "translation_api",
                "retryAfterMs", 10000,
                "queuePosition", 5));
        TaskResult result = worker.execute(task);

        assertEquals(10000, result.getOutputData().get("estimatedWaitMs"));
        assertEquals(5, result.getOutputData().get("queuePosition"));
    }

    @Test
    void handlesNullToolName() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", null);
        input.put("retryAfterMs", 5000);
        input.put("queuePosition", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("q-fixed-001", result.getOutputData().get("queueId"));
    }

    @Test
    void handlesMissingRetryAfterMs() {
        Task task = taskWith(Map.of("toolName", "translation_api", "queuePosition", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5000, result.getOutputData().get("estimatedWaitMs"));
    }

    @Test
    void handlesMissingQueuePosition() {
        Task task = taskWith(Map.of("toolName", "translation_api", "retryAfterMs", 5000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("queuePosition"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("q-fixed-001", result.getOutputData().get("queueId"));
        assertEquals("queued", result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
