package serviceorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticateWorkerTest {

    private final AuthenticateWorker worker = new AuthenticateWorker();

    @Test
    void taskDefName() {
        assertEquals("so_authenticate", worker.getTaskDefName());
    }

    @Test
    void authenticatesUserSuccessfully() {
        Task task = taskWith(Map.of("userId", "user-42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("jwt-token-abc123", result.getOutputData().get("token"));
        assertEquals(true, result.getOutputData().get("authenticated"));
        assertEquals("user-42", result.getOutputData().get("userId"));
    }

    @Test
    void returnsTokenForDifferentUser() {
        Task task = taskWith(Map.of("userId", "admin-01"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("jwt-token-abc123", result.getOutputData().get("token"));
        assertEquals("admin-01", result.getOutputData().get("userId"));
    }

    @Test
    void outputContainsAuthenticatedFlag() {
        Task task = taskWith(Map.of("userId", "user-99"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("authenticated"));
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("userId"));
    }

    @Test
    void handlesMissingUserId() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("userId"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("userId", "test-user"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("token"));
        assertTrue(result.getOutputData().containsKey("authenticated"));
        assertTrue(result.getOutputData().containsKey("userId"));
    }

    @Test
    void handlesEmptyStringUserId() {
        Task task = taskWith(Map.of("userId", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("userId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
