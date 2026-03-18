package databasebackup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VerifyIntegrityTest {

    private final VerifyIntegrity worker = new VerifyIntegrity();

    @Test
    void taskDefName() {
        assertEquals("backup_verify_integrity", worker.getTaskDefName());
    }

    @Test
    void validBackupPassesAllChecks() {
        // Generate a consistent snapshot to verify against
        String dbName = "orders_production";
        String timestamp = "20250315T120000Z";
        String checksum = TakeSnapshot.computeDeterministicChecksum(dbName, timestamp);
        long sizeBytes = TakeSnapshot.computeDeterministicSize(dbName);
        String filename = dbName + "_" + timestamp + ".sql.gz";

        Task task = taskWith(filename, checksum, "SHA-256", sizeBytes, true, "postgresql", dbName);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals(4, result.getOutputData().get("checksPassed"));
        assertEquals(4, result.getOutputData().get("checksTotal"));
    }

    @Test
    void wrongChecksumFailsVerification() {
        String dbName = "orders_production";
        String timestamp = "20250315T120000Z";
        String filename = dbName + "_" + timestamp + ".sql.gz";
        long sizeBytes = TakeSnapshot.computeDeterministicSize(dbName);

        Task task = taskWith(filename, "badc0ffee" + "0".repeat(55), "SHA-256",
                sizeBytes, true, "postgresql", dbName);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("verified"));

        // Checksum check should fail, others should pass
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) result.getOutputData().get("checks");
        Map<String, Object> checksumCheck = checks.stream()
                .filter(c -> "checksum".equals(c.get("check")))
                .findFirst().orElseThrow();
        assertEquals(false, checksumCheck.get("passed"));
    }

    @Test
    void zeroSizeFailsVerification() {
        String dbName = "orders_production";
        String timestamp = "20250315T120000Z";
        String checksum = TakeSnapshot.computeDeterministicChecksum(dbName, timestamp);
        String filename = dbName + "_" + timestamp + ".sql.gz";

        Task task = taskWith(filename, checksum, "SHA-256", 0, true, "postgresql", dbName);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("verified"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) result.getOutputData().get("checks");
        Map<String, Object> sizeCheck = checks.stream()
                .filter(c -> "fileSize".equals(c.get("check")))
                .findFirst().orElseThrow();
        assertEquals(false, sizeCheck.get("passed"));
    }

    @Test
    void excessivelySizeFailsVerification() {
        String dbName = "orders_production";
        String timestamp = "20250315T120000Z";
        String checksum = TakeSnapshot.computeDeterministicChecksum(dbName, timestamp);
        String filename = dbName + "_" + timestamp + ".sql.gz";

        Task task = taskWith(filename, checksum, "SHA-256",
                15L * 1024 * 1024 * 1024, true, "postgresql", dbName); // 15GB

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @Test
    void allFourChecksArePerformed() {
        String dbName = "testdb";
        String timestamp = "20250315T120000Z";
        String checksum = TakeSnapshot.computeDeterministicChecksum(dbName, timestamp);
        long sizeBytes = TakeSnapshot.computeDeterministicSize(dbName);
        String filename = dbName + "_" + timestamp + ".sql.gz";

        Task task = taskWith(filename, checksum, "SHA-256", sizeBytes, true, "postgresql", dbName);

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) result.getOutputData().get("checks");
        assertEquals(4, checks.size());

        Set<String> checkNames = new HashSet<>();
        for (Map<String, Object> c : checks) {
            checkNames.add((String) c.get("check"));
        }
        assertTrue(checkNames.contains("checksum"));
        assertTrue(checkNames.contains("fileSize"));
        assertTrue(checkNames.contains("compression"));
        assertTrue(checkNames.contains("trialRestore"));
    }

    @Test
    void filenameIsPassedThrough() {
        String dbName = "testdb";
        String timestamp = "20250315T120000Z";
        String checksum = TakeSnapshot.computeDeterministicChecksum(dbName, timestamp);
        String filename = dbName + "_" + timestamp + ".sql.gz";

        Task task = taskWith(filename, checksum, "SHA-256",
                TakeSnapshot.computeDeterministicSize(dbName), true, "postgresql", dbName);

        TaskResult result = worker.execute(task);

        assertEquals(filename, result.getOutputData().get("filename"));
    }

    @Test
    void trialRestoreCheckHasMethodField() {
        String dbName = "testdb";
        String timestamp = "20250315T120000Z";
        String checksum = TakeSnapshot.computeDeterministicChecksum(dbName, timestamp);
        long sizeBytes = TakeSnapshot.computeDeterministicSize(dbName);
        String filename = dbName + "_" + timestamp + ".sql.gz";

        Task task = taskWith(filename, checksum, "SHA-256", sizeBytes, true, "postgresql", dbName);

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) result.getOutputData().get("checks");
        Map<String, Object> restoreCheck = checks.stream()
                .filter(c -> "trialRestore".equals(c.get("check")))
                .findFirst().orElseThrow();
        assertNotNull(restoreCheck.get("method"),
                "Trial restore check should report the method used (pg_restore_list, header_validation, or size_check)");
    }

    private Task taskWith(String filename, String checksum, String checksumAlgorithm,
                          long sizeBytes, boolean compressed, String databaseType, String databaseName) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("filename", filename);
        input.put("checksum", checksum);
        input.put("checksumAlgorithm", checksumAlgorithm);
        input.put("sizeBytes", sizeBytes);
        input.put("compressed", compressed);
        input.put("databaseType", databaseType);
        input.put("databaseName", databaseName);
        task.setInputData(input);
        return task;
    }
}
