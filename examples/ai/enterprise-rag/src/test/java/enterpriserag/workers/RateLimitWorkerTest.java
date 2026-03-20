package enterpriserag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitWorkerTest {

    private final RateLimitWorker worker = new RateLimitWorker();

    @Test
    void taskDefName() {
        assertEquals("er_rate_limit", worker.getTaskDefName());
    }

    @Test
    void returnsRateLimitDetails() {
        Task task = taskWith(Map.of("userId", "user-42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("allowed"));
        assertEquals(12, result.getOutputData().get("current"));
        assertEquals(60, result.getOutputData().get("limit"));
        assertEquals(48, result.getOutputData().get("remaining"));
    }

    @Test
    void allowedIsTrue() {
        Task task = taskWith(Map.of("userId", "user-99"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("allowed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
