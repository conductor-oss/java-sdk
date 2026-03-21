package multifactorauth.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GrantAccessWorkerTest {
    private final GrantAccessWorker worker = new GrantAccessWorker();

    @Test void taskDefName() { assertEquals("mfa_grant", worker.getTaskDefName()); }
    @Test void grantsAccess() {
        Task task = taskWith(Map.of("userId", "USR-123", "factorVerified", true));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("accessGranted"));
    }
    @Test void includesToken() {
        Task task = taskWith(Map.of("userId", "USR-123", "factorVerified", true));
        TaskResult r = worker.execute(task);
        assertTrue(r.getOutputData().get("token").toString().startsWith("mfa_tok_"));
    }
    @Test void includesExpiry() {
        Task task = taskWith(Map.of("userId", "USR-123", "factorVerified", true));
        TaskResult r = worker.execute(task);
        assertEquals(5500, r.getOutputData().get("expiresIn"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
