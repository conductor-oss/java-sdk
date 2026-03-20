package accountdeletion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DeleteAccountWorkerTest {
    private final DeleteAccountWorker worker = new DeleteAccountWorker();

    @Test void taskDefName() { assertEquals("acd_delete", worker.getTaskDefName()); }

    @Test void deletesAccount() {
        Task task = taskWith(Map.of("userId", "USR-123", "backupId", "BKP-ABC"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deleted"));
    }

    @Test void clearsMultipleTables() {
        Task task = taskWith(Map.of("userId", "USR-123", "backupId", "BKP-ABC"));
        TaskResult result = worker.execute(task);
        assertEquals(12, result.getOutputData().get("tablesCleared"));
    }

    @Test void includesDeletedAt() {
        Task task = taskWith(Map.of("userId", "USR-123", "backupId", "BKP-ABC"));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("deletedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
