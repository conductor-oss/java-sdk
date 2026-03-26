package webhookratelimiting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckRateWorkerTest {

    private final CheckRateWorker worker = new CheckRateWorker();

    @Test
    void taskDefName() {
        assertEquals("wl_check_rate", worker.getTaskDefName());
    }

    @Test
    void allowsWhenRateUnderLimit() {
        Task task = taskWith(Map.of("senderId", "sender-1", "rateLimit", 100));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("allowed", result.getOutputData().get("decision"));
        assertEquals(45, result.getOutputData().get("currentRate"));
        assertEquals(100, result.getOutputData().get("limit"));
        assertEquals(0, result.getOutputData().get("retryAfterMs"));
    }

    @Test
    void throttlesWhenRateAtLimit() {
        Task task = taskWith(Map.of("senderId", "sender-1", "rateLimit", 45));
        TaskResult result = worker.execute(task);

        assertEquals("throttled", result.getOutputData().get("decision"));
        assertEquals(45, result.getOutputData().get("currentRate"));
        assertEquals(45, result.getOutputData().get("limit"));
        assertEquals(60000, result.getOutputData().get("retryAfterMs"));
    }

    @Test
    void throttlesWhenRateBelowCurrentRate() {
        Task task = taskWith(Map.of("senderId", "sender-1", "rateLimit", 10));
        TaskResult result = worker.execute(task);

        assertEquals("throttled", result.getOutputData().get("decision"));
        assertEquals(10, result.getOutputData().get("limit"));
    }

    @Test
    void parsesStringRateLimit() {
        Task task = taskWith(Map.of("senderId", "sender-1", "rateLimit", "50"));
        TaskResult result = worker.execute(task);

        assertEquals("allowed", result.getOutputData().get("decision"));
        assertEquals(50, result.getOutputData().get("limit"));
    }

    @Test
    void defaultsLimitTo100WhenMissing() {
        Task task = taskWith(Map.of("senderId", "sender-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("allowed", result.getOutputData().get("decision"));
        assertEquals(100, result.getOutputData().get("limit"));
    }

    @Test
    void handlesNullSenderId() {
        Map<String, Object> input = new HashMap<>();
        input.put("senderId", null);
        input.put("rateLimit", 100);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("allowed", result.getOutputData().get("decision"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("allowed", result.getOutputData().get("decision"));
        assertEquals(45, result.getOutputData().get("currentRate"));
        assertEquals(100, result.getOutputData().get("limit"));
    }

    @Test
    void handlesInvalidRateLimitString() {
        Task task = taskWith(Map.of("senderId", "sender-1", "rateLimit", "not-a-number"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(100, result.getOutputData().get("limit"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
