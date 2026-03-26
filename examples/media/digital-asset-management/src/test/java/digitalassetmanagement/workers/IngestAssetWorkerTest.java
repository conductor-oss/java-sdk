package digitalassetmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngestAssetWorkerTest {

    private final IngestAssetWorker worker = new IngestAssetWorker();

    @Test
    void taskDefName() {
        assertEquals("dam_ingest_asset", worker.getTaskDefName());
    }

    @Test
    void ingestsImageAsset() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-001",
                "assetType", "image",
                "sourceUrl", "https://uploads.example.com/test.psd"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("storagePath"));
        assertEquals(85, result.getOutputData().get("fileSizeMb"));
    }

    @Test
    void returnsChecksum() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-002",
                "assetType", "video",
                "sourceUrl", "https://uploads.example.com/test.mp4"));
        TaskResult result = worker.execute(task);

        assertEquals("sha256:abc123def456", result.getOutputData().get("checksum"));
    }

    @Test
    void returnsDimensions() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-003",
                "assetType", "image",
                "sourceUrl", "https://uploads.example.com/photo.jpg"));
        TaskResult result = worker.execute(task);

        assertEquals("4000x3000", result.getOutputData().get("dimensions"));
    }

    @Test
    void returnsStoragePath() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-004",
                "assetType", "document",
                "sourceUrl", "https://uploads.example.com/doc.pdf"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().get("storagePath").toString().startsWith("s3://"));
    }

    @Test
    void handlesNullAssetType() {
        Map<String, Object> input = new HashMap<>();
        input.put("assetId", "ASSET-005");
        input.put("assetType", null);
        input.put("sourceUrl", "https://uploads.example.com/file.bin");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullAssetId() {
        Map<String, Object> input = new HashMap<>();
        input.put("assetId", null);
        input.put("assetType", "image");
        input.put("sourceUrl", "https://uploads.example.com/test.png");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("storagePath"));
        assertNotNull(result.getOutputData().get("checksum"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
