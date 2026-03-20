package accountdeletion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyDeletionWorkerTest {
    private final VerifyDeletionWorker worker = new VerifyDeletionWorker();

    @Test void taskDefName() { assertEquals("acd_verify", worker.getTaskDefName()); }

    @Test void verifiesIdentity() {
        Task task = taskWith(Map.of("userId", "USR-123", "reason", "no_longer_needed"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
    }

    @Test void completesSuccessfully() {
        Task task = taskWith(Map.of("userId", "USR-456", "reason", "privacy"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
