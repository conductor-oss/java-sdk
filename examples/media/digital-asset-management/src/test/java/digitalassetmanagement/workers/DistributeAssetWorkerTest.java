package digitalassetmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DistributeAssetWorkerTest {

    private final DistributeAssetWorker worker = new DistributeAssetWorker();

    @Test
    void taskDefName() {
        assertEquals("dam_distribute_asset", worker.getTaskDefName());
    }

    @Test
    void distributesToChannels() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-001",
                "assetUrl", "https://dam.example.com/assets/001/v1",
                "projectId", "PROJ-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("distributedTo"));
    }

    @Test
    void distributesToMultipleChannels() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-002",
                "assetUrl", "https://dam.example.com/assets/002/v1",
                "projectId", "PROJ-002"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> channels = (List<String>) result.getOutputData().get("distributedTo");
        assertEquals(3, channels.size());
        assertTrue(channels.contains("website"));
    }

    @Test
    void returnsCdnUrls() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-003",
                "assetUrl", "https://dam.example.com/assets/003/v1",
                "projectId", "PROJ-003"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> cdnUrls = (List<String>) result.getOutputData().get("cdnUrls");
        assertNotNull(cdnUrls);
        assertEquals(2, cdnUrls.size());
    }

    @Test
    void cdnUrlsContainCorrectDomain() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-004",
                "assetUrl", "https://dam.example.com/assets/004/v1",
                "projectId", "PROJ-004"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> cdnUrls = (List<String>) result.getOutputData().get("cdnUrls");
        assertTrue(cdnUrls.get(0).contains("cdn.example.com"));
    }

    @Test
    void handlesNullProjectId() {
        Map<String, Object> input = new HashMap<>();
        input.put("assetId", "ASSET-005");
        input.put("assetUrl", "https://dam.example.com/assets/005/v1");
        input.put("projectId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("distributedTo"));
    }

    @Test
    void includesMobileAppChannel() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-006",
                "assetUrl", "https://dam.example.com/assets/006/v1",
                "projectId", "PROJ-006"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> channels = (List<String>) result.getOutputData().get("distributedTo");
        assertTrue(channels.contains("mobile_app"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
