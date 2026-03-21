package sociallogin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CreateSessionWorkerTest {
    private final CreateSessionWorker worker = new CreateSessionWorker();
    @Test void taskDefName() { assertEquals("slo_session", worker.getTaskDefName()); }
    @Test void createsSession() {
        TaskResult r = worker.execute(taskWith(Map.of("userId", "USR-123")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("sessionToken").toString().startsWith("sess_"));
    }
    @Test void includesExpiry() {
        TaskResult r = worker.execute(taskWith(Map.of("userId", "USR-123")));
        assertEquals(86400, r.getOutputData().get("expiresIn"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
