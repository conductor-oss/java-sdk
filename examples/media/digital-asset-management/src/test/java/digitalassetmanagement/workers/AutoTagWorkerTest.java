package digitalassetmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AutoTagWorkerTest {

    private final AutoTagWorker worker = new AutoTagWorker();

    @Test
    void taskDefName() {
        assertEquals("dam_auto_tag", worker.getTaskDefName());
    }

    @Test
    void tagsImageAsset() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-001",
                "assetType", "image",
                "storagePath", "s3://dam/assets/test.psd"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("tags"));
    }

    @Test
    void returnsMultipleTags() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-002",
                "assetType", "image",
                "storagePath", "s3://dam/assets/test.jpg"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) result.getOutputData().get("tags");
        assertTrue(tags.size() >= 4);
        assertTrue(tags.contains("product_photo"));
    }

    @Test
    void returnsAiConfidence() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-003",
                "assetType", "video",
                "storagePath", "s3://dam/assets/vid.mp4"));
        TaskResult result = worker.execute(task);

        assertEquals(0.91, result.getOutputData().get("aiConfidence"));
    }

    @Test
    void returnsColorPalette() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-004",
                "assetType", "image",
                "storagePath", "s3://dam/assets/photo.png"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> palette = (List<String>) result.getOutputData().get("colorPalette");
        assertNotNull(palette);
        assertFalse(palette.isEmpty());
    }

    @Test
    void handlesNullAssetType() {
        Map<String, Object> input = new HashMap<>();
        input.put("assetId", "ASSET-005");
        input.put("assetType", null);
        input.put("storagePath", "s3://dam/assets/file.bin");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("tags"));
    }

    @Test
    void tagsContainSummerTag() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-006",
                "assetType", "image",
                "storagePath", "s3://dam/assets/summer.jpg"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) result.getOutputData().get("tags");
        assertTrue(tags.contains("summer_2026"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
