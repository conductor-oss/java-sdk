package databasebackup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TakeSnapshotTest {

    private final TakeSnapshot worker = new TakeSnapshot();

    @Test
    void taskDefName() {
        assertEquals("backup_take_snapshot", worker.getTaskDefName());
    }

    @Test
    void postgresqlRunsInMockModeWithoutPgPassword() {
        // Without PGPASSWORD, should run in mock mode (not throw)
        if (System.getenv("PGPASSWORD") == null) {
            Task task = taskWith("postgresql", "db.internal", "orders_production");
            TaskResult result = worker.execute(task);
            assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        }
    }

    @Test
    void checksumIsDeterministicForSameInputs() {
        String c1 = TakeSnapshot.computeDeterministicChecksum("mydb", "20250101T120000Z");
        String c2 = TakeSnapshot.computeDeterministicChecksum("mydb", "20250101T120000Z");

        assertEquals(c1, c2);
        assertEquals(64, c1.length(), "SHA-256 hex string should be 64 characters");
    }

    @Test
    void checksumDiffersForDifferentDatabases() {
        String c1 = TakeSnapshot.computeDeterministicChecksum("db1", "20250101T120000Z");
        String c2 = TakeSnapshot.computeDeterministicChecksum("db2", "20250101T120000Z");

        assertNotEquals(c1, c2);
    }

    @Test
    void checksumDiffersForDifferentTimestamps() {
        String c1 = TakeSnapshot.computeDeterministicChecksum("mydb", "20250101T120000Z");
        String c2 = TakeSnapshot.computeDeterministicChecksum("mydb", "20250201T120000Z");

        assertNotEquals(c1, c2);
    }

    @Test
    void fileSizeIsDeterministic() {
        long size1 = TakeSnapshot.computeDeterministicSize("orders_production");
        long size2 = TakeSnapshot.computeDeterministicSize("orders_production");

        assertEquals(size1, size2);
    }

    @Test
    void fileSizeIsInExpectedRange() {
        long minSize = 50L * 1024 * 1024;  // 50MB
        long maxSize = 2L * 1024 * 1024 * 1024; // 2GB

        for (String db : List.of("orders", "users", "analytics", "cache", "inventory")) {
            long size = TakeSnapshot.computeDeterministicSize(db);
            assertTrue(size >= minSize, "Size for '" + db + "' should be >= 50MB, got: " + size);
            assertTrue(size < maxSize, "Size for '" + db + "' should be < 2GB, got: " + size);
        }
    }

    @Test
    void formatBytesProducesReadableOutput() {
        assertEquals("500 bytes", TakeSnapshot.formatBytes(500));
        assertEquals("1.0 KB", TakeSnapshot.formatBytes(1024));
        assertEquals("1.5 MB", TakeSnapshot.formatBytes(1572864));
        assertEquals("1.0 GB", TakeSnapshot.formatBytes(1073741824));
    }

    @Test
    void computeFileChecksumWorks(@TempDir Path tempDir) throws Exception {
        // Write known content and verify checksum
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "hello world");

        String checksum = TakeSnapshot.computeFileChecksum(testFile);
        assertNotNull(checksum);
        assertEquals(64, checksum.length(), "SHA-256 hex should be 64 chars");
        // SHA-256 of "hello world" is known
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", checksum);
    }

    @Test
    void computeFileChecksumDiffersForDifferentContent(@TempDir Path tempDir) throws Exception {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content A");
        Files.writeString(file2, "content B");

        String checksum1 = TakeSnapshot.computeFileChecksum(file1);
        String checksum2 = TakeSnapshot.computeFileChecksum(file2);

        assertNotEquals(checksum1, checksum2);
    }

    private Task taskWith(String databaseType, String databaseHost, String databaseName) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("databaseType", databaseType);
        input.put("databaseHost", databaseHost);
        input.put("databaseName", databaseName);
        task.setInputData(input);
        return task;
    }
}
