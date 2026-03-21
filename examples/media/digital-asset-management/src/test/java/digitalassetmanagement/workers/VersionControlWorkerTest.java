package digitalassetmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VersionControlWorkerTest {

    private final VersionControlWorker worker = new VersionControlWorker();

    @Test
    void taskDefName() {
        assertEquals("dam_version_control", worker.getTaskDefName());
    }

    @Test
    void createsInitialVersion() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-001",
                "projectId", "PROJ-001",
                "checksum", "sha256:abc123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("1.0.0", result.getOutputData().get("version"));
        assertEquals("initial", result.getOutputData().get("changeType"));
    }

    @Test
    void returnsVersionId() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-002",
                "projectId", "PROJ-002",
                "checksum", "sha256:def456"));
        TaskResult result = worker.execute(task);

        assertEquals("VER-521-001", result.getOutputData().get("versionId"));
    }

    @Test
    void previousVersionIsNull() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-003",
                "projectId", "PROJ-003",
                "checksum", "sha256:ghi789"));
        TaskResult result = worker.execute(task);

        assertNull(result.getOutputData().get("previousVersion"));
    }

    @Test
    void handlesNullProjectId() {
        Map<String, Object> input = new HashMap<>();
        input.put("assetId", "ASSET-004");
        input.put("projectId", null);
        input.put("checksum", "sha256:xyz");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("version"));
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-005",
                "projectId", "PROJ-005",
                "checksum", "sha256:test"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("version"));
        assertNotNull(result.getOutputData().get("changeType"));
        assertNotNull(result.getOutputData().get("versionId"));
    }

    @Test
    void versionFormatIsSemver() {
        Task task = taskWith(Map.of(
                "assetId", "ASSET-006",
                "projectId", "PROJ-006",
                "checksum", "sha256:semver"));
        TaskResult result = worker.execute(task);

        String version = (String) result.getOutputData().get("version");
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
