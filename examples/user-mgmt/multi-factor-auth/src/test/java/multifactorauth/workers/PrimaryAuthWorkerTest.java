package multifactorauth.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PrimaryAuthWorkerTest {
    private final PrimaryAuthWorker worker = new PrimaryAuthWorker();

    @Test void taskDefName() { assertEquals("mfa_primary_auth", worker.getTaskDefName()); }
    @Test void authenticates() {
        Task task = taskWith(Map.of("username", "frank", "password", "pw"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("userId"));
        assertEquals(true, r.getOutputData().get("primaryPassed"));
    }
    @Test void returnsUserId() {
        Task task = taskWith(Map.of("username", "alice", "password", "pw"));
        TaskResult r = worker.execute(task);
        assertEquals("USR-MFA01", r.getOutputData().get("userId"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
