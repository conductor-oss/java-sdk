package authenticationworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateCredentialsWorkerTest {

    private final ValidateCredentialsWorker worker = new ValidateCredentialsWorker();

    @Test
    void taskDefName() {
        assertEquals("auth_validate_credentials", worker.getTaskDefName());
    }

    @Test
    void validatesCredentialsSuccessfully() {
        Task task = taskWith(Map.of("userId", "user-001", "authMethod", "totp"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("VALIDATE_CREDENTIALS-1371", result.getOutputData().get("validate_credentialsId"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void outputContainsId() {
        Task task = taskWith(Map.of("userId", "user-002", "authMethod", "sms"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("validate_credentialsId"));
    }

    @Test
    void outputContainsSuccess() {
        Task task = taskWith(Map.of("userId", "user-003", "authMethod", "biometric"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", null);
        input.put("authMethod", "totp");
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
    void handlesEmptyUserId() {
        Task task = taskWith(Map.of("userId", "", "authMethod", "totp"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("VALIDATE_CREDENTIALS-1371", result.getOutputData().get("validate_credentialsId"));
    }

    @Test
    void deterministicOutput() {
        Task task1 = taskWith(Map.of("userId", "user-001", "authMethod", "totp"));
        Task task2 = taskWith(Map.of("userId", "user-001", "authMethod", "totp"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("validate_credentialsId"),
                     r2.getOutputData().get("validate_credentialsId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
