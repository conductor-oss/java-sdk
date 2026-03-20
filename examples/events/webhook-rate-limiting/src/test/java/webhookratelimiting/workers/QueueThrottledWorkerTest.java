package webhookratelimiting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueueThrottledWorkerTest {

    private final QueueThrottledWorker worker = new QueueThrottledWorker();

    @Test
    void taskDefName() {
        assertEquals("wl_queue_throttled", worker.getTaskDefName());
    }

    @Test
    void queuesThrottledRequest() {
        Task task = taskWith(Map.of("senderId", "partner-api-xyz", "retryAfterMs", 60000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("queued"));
        assertEquals(60000, result.getOutputData().get("retryAfterMs"));
    }

    @Test
    void outputContainsQueuedFlag() {
        Task task = taskWith(Map.of("senderId", "sender-1", "retryAfterMs", 30000));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("queued"));
    }

    @Test
    void preservesRetryAfterMs() {
        Task task = taskWith(Map.of("senderId", "sender-1", "retryAfterMs", 120000));
        TaskResult result = worker.execute(task);

        assertEquals(120000, result.getOutputData().get("retryAfterMs"));
    }

    @Test
    void parsesStringRetryAfterMs() {
        Task task = taskWith(Map.of("senderId", "sender-1", "retryAfterMs", "45000"));
        TaskResult result = worker.execute(task);

        assertEquals(45000, result.getOutputData().get("retryAfterMs"));
    }

    @Test
    void defaultsRetryAfterMsWhenMissing() {
        Task task = taskWith(Map.of("senderId", "sender-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("queued"));
        assertEquals(60000, result.getOutputData().get("retryAfterMs"));
    }

    @Test
    void handlesNullSenderId() {
        Map<String, Object> input = new HashMap<>();
        input.put("senderId", null);
        input.put("retryAfterMs", 60000);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("queued"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("queued"));
        assertEquals(60000, result.getOutputData().get("retryAfterMs"));
    }

    @Test
    void handlesZeroRetryAfterMs() {
        Task task = taskWith(Map.of("senderId", "sender-1", "retryAfterMs", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("queued"));
        assertEquals(0, result.getOutputData().get("retryAfterMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
