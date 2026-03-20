package multifactorauth.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyFactorWorkerTest {
    private final VerifyFactorWorker worker = new VerifyFactorWorker();

    @Test void taskDefName() { assertEquals("mfa_verify_factor", worker.getTaskDefName()); }
    @Test void verifiesFactor() {
        Task task = taskWith(Map.of("userId", "USR-123", "method", "totp"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("verified"));
    }
    @Test void tracksAttempts() {
        Task task = taskWith(Map.of("userId", "USR-123", "method", "sms"));
        TaskResult r = worker.execute(task);
        assertEquals(1, r.getOutputData().get("attempts"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
