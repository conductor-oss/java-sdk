package userregistration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmWorkerTest {

    private final ConfirmWorker worker = new ConfirmWorker();

    @Test
    void taskDefName() {
        assertEquals("ur_confirm", worker.getTaskDefName());
    }

    @Test
    void sendsConfirmation() {
        Task task = taskWith(Map.of("userId", "USR-123", "email", "bob@test.com"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmationSent"));
    }

    @Test
    void generatesToken() {
        Task task = taskWith(Map.of("userId", "USR-123", "email", "bob@test.com"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().get("token").toString().startsWith("tok_"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
