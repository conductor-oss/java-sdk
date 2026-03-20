package gdprdatadeletion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyIdentityWorkerTest {

    private final VerifyIdentityWorker worker = new VerifyIdentityWorker();

    @Test
    void taskDefName() {
        assertEquals("gr_verify_identity", worker.getTaskDefName());
    }

    @Test
    void verifiesWithValidToken() {
        Task task = taskWith(Map.of("userId", "USR-001", "verificationToken", "valid-token"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
    }

    @Test
    void failsWithEmptyToken() {
        Task task = taskWith(Map.of("userId", "USR-001", "verificationToken", ""));
        TaskResult result = worker.execute(task);
        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void failsWithMissingToken() {
        Task task = taskWith(Map.of("userId", "USR-001"));
        TaskResult result = worker.execute(task);
        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void returnsVerificationMethod() {
        Task task = taskWith(Map.of("userId", "USR-001", "verificationToken", "token"));
        TaskResult result = worker.execute(task);
        assertEquals("token_verification", result.getOutputData().get("method"));
    }

    @Test
    void handlesNullToken() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", "USR-001");
        input.put("verificationToken", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(false, result.getOutputData().get("verified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
