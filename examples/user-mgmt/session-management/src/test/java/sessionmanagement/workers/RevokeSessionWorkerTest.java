package sessionmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RevokeSessionWorkerTest {
    private final RevokeSessionWorker worker = new RevokeSessionWorker();

    @Test void taskDefName() { assertEquals("ses_revoke", worker.getTaskDefName()); }

    @Test void revokesSession() {
        Task task = taskWith(Map.of("sessionId", "SES-ABC", "userId", "USR-123"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("revoked"));
    }

    @Test void includesRevokedAt() {
        Task task = taskWith(Map.of("sessionId", "SES-ABC", "userId", "USR-123"));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("revokedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
