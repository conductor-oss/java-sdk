package ratelimitermicroservice.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckQuotaWorkerTest {

    private final CheckQuotaWorker worker = new CheckQuotaWorker();

    @Test
    void taskDefName() {
        assertEquals("rl_check_quota", worker.getTaskDefName());
    }

    @Test
    void returnsAllowedTrue() {
        Task task = taskWith(Map.of("clientId", "client-99", "endpoint", "/api/orders"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("allowed"));
    }

    @Test
    void returnsRemaining58() {
        Task task = taskWith(Map.of("clientId", "client-99", "endpoint", "/api/orders"));
        TaskResult result = worker.execute(task);

        assertEquals(58, result.getOutputData().get("remaining"));
    }

    @Test
    void returnsLimit100() {
        Task task = taskWith(Map.of("clientId", "client-1", "endpoint", "/api/users"));
        TaskResult result = worker.execute(task);

        assertEquals(100, result.getOutputData().get("limit"));
    }

    @Test
    void returnsWindow1m() {
        Task task = taskWith(Map.of("clientId", "client-5", "endpoint", "/api/data"));
        TaskResult result = worker.execute(task);

        assertEquals("1m", result.getOutputData().get("window"));
    }

    @Test
    void handlesNullClientId() {
        Map<String, Object> input = new HashMap<>();
        input.put("clientId", null);
        input.put("endpoint", "/api/test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("allowed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputHasAllFourFields() {
        Task task = taskWith(Map.of("clientId", "c", "endpoint", "/e"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("allowed"));
        assertTrue(result.getOutputData().containsKey("remaining"));
        assertTrue(result.getOutputData().containsKey("limit"));
        assertTrue(result.getOutputData().containsKey("window"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
