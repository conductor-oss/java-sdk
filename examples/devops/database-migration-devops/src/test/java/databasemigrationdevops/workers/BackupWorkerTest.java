package databasemigrationdevops.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BackupWorkerTest {

    private final BackupWorker worker = new BackupWorker();

    @Test
    void taskDefName() {
        assertEquals("dbm_backup", worker.getTaskDefName());
    }

    @Test
    void backupsWithValidInputs() {
        Task task = taskWith(Map.of("database", "orders-prod", "migrationVersion", "V042"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("BACKUP-1363", result.getOutputData().get("backupId"));
        assertEquals(true, result.getOutputData().get("success"));
        assertEquals("orders-prod", result.getOutputData().get("database"));
        assertEquals("V042", result.getOutputData().get("migrationVersion"));
    }

    @Test
    void handlesNullDatabase() {
        Map<String, Object> input = new HashMap<>();
        input.put("database", null);
        input.put("migrationVersion", "V001");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-db", result.getOutputData().get("database"));
    }

    @Test
    void handlesNullMigrationVersion() {
        Map<String, Object> input = new HashMap<>();
        input.put("database", "db");
        input.put("migrationVersion", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("V000", result.getOutputData().get("migrationVersion"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-db", result.getOutputData().get("database"));
        assertEquals("V000", result.getOutputData().get("migrationVersion"));
    }

    @Test
    void alwaysReturnsSuccess() {
        Task task = taskWith(Map.of("database", "db", "migrationVersion", "V1"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void outputContainsBackupId() {
        Task task = taskWith(Map.of("database", "db", "migrationVersion", "V1"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("backupId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
