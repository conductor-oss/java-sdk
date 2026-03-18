package databasebackup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CleanupOldBackupsTest {

    private final CleanupOldBackups worker = new CleanupOldBackups();

    @Test
    void taskDefName() {
        assertEquals("backup_cleanup_old", worker.getTaskDefName());
    }

    @Test
    void retentionPolicyDeletesOldBackups() {
        // Use a short retention (7 days) so some of the generated 60-day spread is deleted
        Task task = taskWith("orders_production", "s3", "my-backups", 7, 100);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        int existing = (int) result.getOutputData().get("existingCount");
        int deleted = (int) result.getOutputData().get("deletedCount");
        int retained = (int) result.getOutputData().get("retainedCount");

        assertTrue(existing > 0, "Should have found existing backups");
        assertEquals(existing, deleted + retained, "deleted + retained should equal existing");
        assertTrue(deleted > 0, "With 7-day retention over 60-day spread, some should be deleted");
    }

    @Test
    void maxBackupsLimitEnforced() {
        // Allow 3 max backups with long retention — should still cap at 3
        Task task = taskWith("orders_production", "s3", "my-backups", 365, 3);

        TaskResult result = worker.execute(task);

        int retained = (int) result.getOutputData().get("retainedCount");
        assertTrue(retained <= 3, "Should retain at most 3 backups, got: " + retained);
    }

    @Test
    void freedBytesIsPositiveWhenDeletingBackups() {
        Task task = taskWith("orders_production", "s3", "my-backups", 7, 100);

        TaskResult result = worker.execute(task);

        int deleted = (int) result.getOutputData().get("deletedCount");
        if (deleted > 0) {
            long freed = (long) result.getOutputData().get("freedBytes");
            assertTrue(freed > 0, "Should free some space when deleting backups");
        }
    }

    @Test
    void generatedBackupsAreDeterministic() {
        List<Map<String, Object>> backups1 = CleanupOldBackups.generateExistingBackups("mydb");
        List<Map<String, Object>> backups2 = CleanupOldBackups.generateExistingBackups("mydb");

        assertEquals(backups1.size(), backups2.size());
    }

    @Test
    void differentDatabasesProduceDifferentBackupCounts() {
        // Due to hash-based generation, different names may produce different counts
        Set<Integer> counts = new HashSet<>();
        for (String name : List.of("alpha", "beta", "gamma", "delta", "epsilon",
                "zeta", "eta", "theta", "iota", "kappa")) {
            counts.add(CleanupOldBackups.generateExistingBackups(name).size());
        }
        // With 10 different names, we should get at least 2 distinct counts
        assertTrue(counts.size() >= 2, "Expected variation in backup counts across different databases");
    }

    @Test
    void deletedFilesListMatchesCount() {
        Task task = taskWith("test_db", "s3", "backups", 7, 100);

        TaskResult result = worker.execute(task);

        int deletedCount = (int) result.getOutputData().get("deletedCount");
        @SuppressWarnings("unchecked")
        List<String> deletedFiles = (List<String>) result.getOutputData().get("deletedFiles");

        assertEquals(deletedCount, deletedFiles.size());
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith("testdb", "s3", "backups", 30, 10);

        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("existingCount"));
        assertNotNull(result.getOutputData().get("deletedCount"));
        assertNotNull(result.getOutputData().get("retainedCount"));
        assertNotNull(result.getOutputData().get("freedBytes"));
        assertNotNull(result.getOutputData().get("freedFormatted"));
        assertNotNull(result.getOutputData().get("retentionDays"));
        assertNotNull(result.getOutputData().get("maxBackups"));
        assertNotNull(result.getOutputData().get("deletedFiles"));
    }

    private Task taskWith(String databaseName, String storageType, String bucket,
                          int retentionDays, int maxBackups) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("databaseName", databaseName);
        input.put("storageType", storageType);
        input.put("bucket", bucket);
        input.put("retentionDays", retentionDays);
        input.put("maxBackups", maxBackups);
        task.setInputData(input);
        return task;
    }
}
