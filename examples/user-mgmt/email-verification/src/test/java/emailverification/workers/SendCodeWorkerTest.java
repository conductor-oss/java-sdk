package emailverification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SendCodeWorkerTest {

    private final SendCodeWorker worker = new SendCodeWorker();

    @Test
    void taskDefName() {
        assertEquals("emv_send_code", worker.getTaskDefName());
    }

    @Test
    void sendsVerificationCode() {
        Task task = taskWith(Map.of("email", "diana@example.com", "userId", "USR-123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("verificationCode"));
    }

    @Test
    void includesSentAt() {
        Task task = taskWith(Map.of("email", "test@test.com", "userId", "USR-456"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("sentAt"));
    }

    @Test
    void includesExpiresIn() {
        Task task = taskWith(Map.of("email", "test@test.com", "userId", "USR-456"));
        TaskResult result = worker.execute(task);

        assertEquals(600, result.getOutputData().get("expiresIn"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
