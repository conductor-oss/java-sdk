package sessionmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CreateSessionWorkerTest {
    private final CreateSessionWorker worker = new CreateSessionWorker();

    @Test void taskDefName() { assertEquals("ses_create", worker.getTaskDefName()); }

    @Test void createsSession() {
        Task task = taskWith(Map.of("userId", "USR-123", "deviceInfo", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().get("sessionId").toString().startsWith("SES-"));
    }

    @Test void includesToken() {
        Task task = taskWith(Map.of("userId", "USR-123", "deviceInfo", Map.of()));
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().get("token").toString().startsWith("jwt_"));
    }

    @Test void includesExpiresIn() {
        Task task = taskWith(Map.of("userId", "USR-123", "deviceInfo", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(3600, result.getOutputData().get("expiresIn"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
