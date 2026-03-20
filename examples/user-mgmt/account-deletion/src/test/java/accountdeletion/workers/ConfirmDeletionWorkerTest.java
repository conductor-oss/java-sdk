package accountdeletion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ConfirmDeletionWorkerTest {
    private final ConfirmDeletionWorker worker = new ConfirmDeletionWorker();

    @Test void taskDefName() { assertEquals("acd_confirm", worker.getTaskDefName()); }

    @Test void sendsConfirmation() {
        Task task = taskWith(Map.of("userId", "USR-123", "deleted", true));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmationSent"));
    }

    @Test void isGdprCompliant() {
        Task task = taskWith(Map.of("userId", "USR-123", "deleted", true));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("gdprCompliant"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
