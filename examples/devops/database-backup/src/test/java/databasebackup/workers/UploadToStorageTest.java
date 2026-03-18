package databasebackup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UploadToStorageTest {

    private final UploadToStorage worker = new UploadToStorage();

    @Test
    void taskDefName() {
        assertEquals("backup_upload_to_storage", worker.getTaskDefName());
    }

    @Test
    void s3UploadProducesCorrectUri() {
        Task task = taskWith("orders_20250315T120000Z.sql.gz", 500_000_000L, "abc123",
                Map.of("type", "s3", "bucket", "acme-backups", "path", "postgresql/orders", "region", "us-west-2"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("s3://acme-backups/postgresql/orders/orders_20250315T120000Z.sql.gz",
                result.getOutputData().get("storageUri"));
        assertEquals("s3", result.getOutputData().get("storageType"));
        assertEquals("acme-backups", result.getOutputData().get("bucket"));
        assertEquals("us-west-2", result.getOutputData().get("region"));
    }

    @Test
    void gcsUploadProducesCorrectUri() {
        Task task = taskWith("mydb_20250315T120000Z.sql.gz", 100_000_000L, "def456",
                Map.of("type", "gcs", "bucket", "gcp-backups", "path", "mysql"));

        TaskResult result = worker.execute(task);

        assertEquals("gs://gcp-backups/mysql/mydb_20250315T120000Z.sql.gz",
                result.getOutputData().get("storageUri"));
    }

    @Test
    void azureBlobUploadProducesCorrectUri() {
        Task task = taskWith("mydb_20250315T120000Z.sql.gz", 100_000_000L, "ghi789",
                Map.of("type", "azure-blob", "bucket", "azureaccount", "path", "backups"));

        TaskResult result = worker.execute(task);

        assertEquals("https://azureaccount.blob.core.windows.net/backups/mydb_20250315T120000Z.sql.gz",
                result.getOutputData().get("storageUri"));
    }

    @Test
    void localStorageProducesCorrectUri() {
        Task task = taskWith("mydb_20250315T120000Z.sql.gz", 100_000_000L, "jkl012",
                Map.of("type", "local", "bucket", "ignored", "path", "daily"));

        TaskResult result = worker.execute(task);

        assertEquals("file:///var/backups/daily/mydb_20250315T120000Z.sql.gz",
                result.getOutputData().get("storageUri"));
    }

    @Test
    void etagIsDeterministic() {
        String etag1 = UploadToStorage.computeEtag("file.sql.gz", "checksum123");
        String etag2 = UploadToStorage.computeEtag("file.sql.gz", "checksum123");

        assertEquals(etag1, etag2);
        assertFalse(etag1.isEmpty());
    }

    @Test
    void etagDiffersForDifferentFiles() {
        String etag1 = UploadToStorage.computeEtag("file1.sql.gz", "checksum123");
        String etag2 = UploadToStorage.computeEtag("file2.sql.gz", "checksum123");

        assertNotEquals(etag1, etag2);
    }

    @Test
    void versionIdIsDeterministic() {
        String v1 = UploadToStorage.computeVersionId("file.sql.gz");
        String v2 = UploadToStorage.computeVersionId("file.sql.gz");

        assertEquals(v1, v2);
        assertTrue(v1.startsWith("v"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith("orders.sql.gz", 500_000_000L, "abc",
                Map.of("type", "s3", "bucket", "backups", "path", "db", "region", "us-east-1"));

        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("storageUri"));
        assertNotNull(result.getOutputData().get("storagePath"));
        assertNotNull(result.getOutputData().get("storageType"));
        assertNotNull(result.getOutputData().get("bucket"));
        assertNotNull(result.getOutputData().get("region"));
        assertNotNull(result.getOutputData().get("sizeBytes"));
        assertNotNull(result.getOutputData().get("uploadDurationMs"));
        assertNotNull(result.getOutputData().get("etag"));
        assertNotNull(result.getOutputData().get("versionId"));
        assertNotNull(result.getOutputData().get("filename"));
        assertNotNull(result.getOutputData().get("checksum"));
        assertNotNull(result.getOutputData().get("timestamp"));
    }

    @Test
    void defaultsAppliedWhenStorageConfigMissing() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("filename", "test.sql.gz");
        input.put("sizeBytes", 1000L);
        input.put("checksum", "abc");
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("s3", result.getOutputData().get("storageType"));
        assertEquals("backups", result.getOutputData().get("bucket"));
    }

    @Test
    void buildStorageUriFormatsCorrectly() {
        assertEquals("s3://mybucket/path/file.gz",
                UploadToStorage.buildStorageUri("s3", "mybucket", "path/file.gz", "us-east-1"));
        assertEquals("gs://mybucket/path/file.gz",
                UploadToStorage.buildStorageUri("gcs", "mybucket", "path/file.gz", "us-east-1"));
        assertEquals("https://mybucket.blob.core.windows.net/path/file.gz",
                UploadToStorage.buildStorageUri("azure-blob", "mybucket", "path/file.gz", "us-east-1"));
        assertEquals("file:///var/backups/path/file.gz",
                UploadToStorage.buildStorageUri("local", "mybucket", "path/file.gz", "us-east-1"));
    }

    private Task taskWith(String filename, long sizeBytes, String checksum, Map<String, Object> storage) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("filename", filename);
        input.put("sizeBytes", sizeBytes);
        input.put("checksum", checksum);
        input.put("storage", storage);
        task.setInputData(input);
        return task;
    }
}
