package ragaccesscontrol.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticateUserWorkerTest {

    private final AuthenticateUserWorker worker = new AuthenticateUserWorker();

    @Test
    void taskDefName() {
        assertEquals("ac_authenticate_user", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void authenticatesValidToken() {
        Task task = taskWith(Map.of(
                "userId", "user-42",
                "authToken", "valid-jwt-token-12345"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("authenticated"));
        assertEquals("user-42", result.getOutputData().get("userId"));

        List<String> roles = (List<String>) result.getOutputData().get("roles");
        assertNotNull(roles);
        assertTrue(roles.contains("engineer"));
        assertTrue(roles.contains("team-lead"));

        assertEquals("confidential", result.getOutputData().get("clearanceLevel"));
    }

    @Test
    void rejectsInvalidToken() {
        Task task = taskWith(Map.of(
                "userId", "user-99",
                "authToken", "bad-token"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("authenticated"));
        assertNotNull(result.getOutputData().get("error"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
