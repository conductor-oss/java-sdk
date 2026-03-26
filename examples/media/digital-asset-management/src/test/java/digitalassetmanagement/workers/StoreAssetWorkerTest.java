package digitalassetmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StoreAssetWorkerTest {

    private final StoreAssetWorker worker = new StoreAssetWorker();

    @Test
    void taskDefName() {
        assertEquals("dam_store_asset", worker.getTaskDefName());
    }

    @Test
    void storesAssetWithTags() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-001",
                "storagePath", "s3://dam/assets/test.psd",
                "tags", List.of("photo", "lifestyle"),
                "version", "1.0.0"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("assetUrl"));
    }

    @Test
    void returnsAssetUrl() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-002",
                "storagePath", "s3://dam/assets/img.jpg",
                "tags", List.of("product"),
                "version", "2.0.0"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().get("assetUrl").toString().contains("dam.example.com"));
    }

    @Test
    void returnsIndexedTrue() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-003",
                "storagePath", "s3://dam/assets/doc.pdf",
                "tags", List.of(),
                "version", "1.0.0"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("indexed"));
    }

    @Test
    void returnsStoredAtTimestamp() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-004",
                "storagePath", "s3://dam/test",
                "tags", List.of("tag1"),
                "version", "1.0.0"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("storedAt"));
    }

    @Test
    void handlesNullVersion() {
        Map<String, Object> input = new HashMap<>();
        input.put("assetId", "ASSET-005");
        input.put("storagePath", "s3://dam/file");
        input.put("tags", List.of("tag"));
        input.put("version", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullTags() {
        Map<String, Object> input = new HashMap<>();
        input.put("assetId", "ASSET-006");
        input.put("storagePath", "s3://dam/file");
        input.put("tags", null);
        input.put("version", "1.0.0");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("assetUrl"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
