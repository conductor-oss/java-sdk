package accountdeletion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class BackupWorkerTest {
    private final BackupWorker worker = new BackupWorker();

    @Test void taskDefName() { assertEquals("acd_backup", worker.getTaskDefName()); }

    @Test void createsBackup() {
        Task task = taskWith(Map.of("userId", "USR-123"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().get("backupId").toString().startsWith("BKP-"));
    }

    @Test void includesRetainDays() {
        Task task = taskWith(Map.of("userId", "USR-123"));
        TaskResult result = worker.execute(task);
        assertEquals(30, result.getOutputData().get("retainDays"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
