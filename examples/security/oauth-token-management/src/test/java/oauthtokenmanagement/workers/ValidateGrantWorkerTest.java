package oauthtokenmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateGrantWorkerTest {

    private final ValidateGrantWorker worker = new ValidateGrantWorker();

    @Test void taskDefName() { assertEquals("otm_validate_grant", worker.getTaskDefName()); }

    @Test void validatesGrantSuccessfully() {
        Task task = taskWith(Map.of("clientId", "app-dashboard", "grantType", "authorization_code", "scope", "read:users"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("VALIDATE_GRANT-1373", result.getOutputData().get("validate_grantId"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test void outputContainsId() {
        Task task = taskWith(Map.of("clientId", "c1", "grantType", "client_credentials"));
        assertNotNull(worker.execute(task).getOutputData().get("validate_grantId"));
    }

    @Test void outputContainsSuccess() {
        Task task = taskWith(Map.of("clientId", "c1", "grantType", "refresh_token"));
        assertEquals(true, worker.execute(task).getOutputData().get("success"));
    }

    @Test void handlesNullClientId() {
        Map<String, Object> input = new HashMap<>(); input.put("clientId", null); input.put("grantType", "code");
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(input)).getStatus());
    }

    @Test void handlesMissingInputs() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of())).getStatus());
    }

    @Test void handlesEmptyClientId() {
        Task task = taskWith(Map.of("clientId", "", "grantType", ""));
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    @Test void deterministicOutput() {
        Task t1 = taskWith(Map.of("clientId", "c1", "grantType", "code"));
        Task t2 = taskWith(Map.of("clientId", "c1", "grantType", "code"));
        assertEquals(worker.execute(t1).getOutputData().get("validate_grantId"),
                     worker.execute(t2).getOutputData().get("validate_grantId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input)); return task;
    }
}
