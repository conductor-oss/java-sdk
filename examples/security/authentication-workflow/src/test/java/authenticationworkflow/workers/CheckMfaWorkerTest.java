package authenticationworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckMfaWorkerTest {

    private final CheckMfaWorker worker = new CheckMfaWorker();

    @Test
    void taskDefName() {
        assertEquals("auth_check_mfa", worker.getTaskDefName());
    }

    @Test
    void checksMfaSuccessfully() {
        Task task = taskWith(Map.of("authMethod", "totp"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("check_mfa"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsCheckMfa() {
        Task task = taskWith(Map.of("authMethod", "sms"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("check_mfa"));
    }

    @Test
    void outputContainsProcessed() {
        Task task = taskWith(Map.of("authMethod", "biometric"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesNullAuthMethod() {
        Map<String, Object> input = new HashMap<>();
        input.put("authMethod", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyAuthMethod() {
        Task task = taskWith(Map.of("authMethod", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void deterministicOutput() {
        Task task1 = taskWith(Map.of("authMethod", "totp"));
        Task task2 = taskWith(Map.of("authMethod", "totp"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("check_mfa"), r2.getOutputData().get("check_mfa"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
