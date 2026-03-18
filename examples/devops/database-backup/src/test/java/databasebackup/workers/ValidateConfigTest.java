package databasebackup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ValidateConfigTest {

    private final ValidateConfig worker = new ValidateConfig();

    @Test
    void taskDefName() {
        assertEquals("backup_validate_config", worker.getTaskDefName());
    }

    @Test
    void validConfigReturnsCompleted() {
        Task task = taskWith(
                Map.of("host", "db.internal", "port", 5432, "name", "mydb", "type", "postgresql", "user", "admin"),
                Map.of("type", "s3", "bucket", "my-backups"),
                Map.of("days", 30, "maxBackups", 10)
        );

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertEquals("postgresql", result.getOutputData().get("databaseType"));
        assertEquals("db.internal", result.getOutputData().get("databaseHost"));
        assertEquals("mydb", result.getOutputData().get("databaseName"));
        assertEquals("s3", result.getOutputData().get("storageType"));
    }

    @Test
    void missingDatabaseConfigFails() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("storage", Map.of("type", "s3", "bucket", "backups"));
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals(false, result.getOutputData().get("valid"));

        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertTrue(errors.stream().anyMatch(e -> e.contains("database")));
    }

    @Test
    void missingStorageConfigFails() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("database", Map.of("host", "db.internal", "port", 5432, "name", "mydb", "type", "postgresql"));
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void unsupportedDbTypeFails() {
        Task task = taskWith(
                Map.of("host", "db.internal", "port", 5432, "name", "mydb", "type", "oracle"),
                Map.of("type", "s3", "bucket", "backups"),
                null
        );

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertTrue(errors.stream().anyMatch(e -> e.contains("oracle") && e.contains("not supported")));
    }

    @Test
    void invalidPortFails() {
        Task task = taskWith(
                Map.of("host", "db.internal", "port", 99999, "name", "mydb", "type", "postgresql"),
                Map.of("type", "s3", "bucket", "backups"),
                null
        );

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertTrue(errors.stream().anyMatch(e -> e.contains("port")));
    }

    @Test
    void missingUserProducesWarning() {
        Task task = taskWith(
                Map.of("host", "db.internal", "port", 5432, "name", "mydb", "type", "postgresql"),
                Map.of("type", "s3", "bucket", "backups"),
                null
        );

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> warnings = (List<String>) result.getOutputData().get("warnings");
        assertTrue(warnings.stream().anyMatch(w -> w.contains("user")));
    }

    @Test
    void defaultRetentionAppliedWhenMissing() {
        Task task = taskWith(
                Map.of("host", "db.internal", "port", 5432, "name", "mydb", "type", "postgresql", "user", "admin"),
                Map.of("type", "s3", "bucket", "backups"),
                null
        );

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(30, result.getOutputData().get("retentionDays"));
        assertEquals(10, result.getOutputData().get("maxBackups"));
    }

    @Test
    void customRetentionIsHonored() {
        Task task = taskWith(
                Map.of("host", "db.internal", "port", 5432, "name", "mydb", "type", "postgresql", "user", "admin"),
                Map.of("type", "s3", "bucket", "backups"),
                Map.of("days", 90, "maxBackups", 25)
        );

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(90, result.getOutputData().get("retentionDays"));
        assertEquals(25, result.getOutputData().get("maxBackups"));
    }

    @Test
    void allSupportedDbTypesAccepted() {
        for (String dbType : List.of("postgresql", "mysql", "mongodb", "redis")) {
            Task task = taskWith(
                    Map.of("host", "db.internal", "port", 5432, "name", "mydb", "type", dbType, "user", "admin"),
                    Map.of("type", "s3", "bucket", "backups"),
                    null
            );
            TaskResult result = worker.execute(task);
            assertEquals(TaskResult.Status.COMPLETED, result.getStatus(),
                    "Expected COMPLETED for database type: " + dbType);
            assertEquals(dbType, result.getOutputData().get("databaseType"));
        }
    }

    @Test
    void allSupportedStorageTypesAccepted() {
        for (String storageType : List.of("s3", "gcs", "azure-blob", "local")) {
            Task task = taskWith(
                    Map.of("host", "db.internal", "port", 5432, "name", "mydb", "type", "postgresql", "user", "admin"),
                    Map.of("type", storageType, "bucket", "backups"),
                    null
            );
            TaskResult result = worker.execute(task);
            assertEquals(TaskResult.Status.COMPLETED, result.getStatus(),
                    "Expected COMPLETED for storage type: " + storageType);
            assertEquals(storageType, result.getOutputData().get("storageType"));
        }
    }

    private Task taskWith(Map<String, Object> database, Map<String, Object> storage, Map<String, Object> retention) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("database", database);
        input.put("storage", storage);
        if (retention != null) {
            input.put("retention", retention);
        }
        task.setInputData(input);
        return task;
    }
}
