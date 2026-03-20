package claimcheck.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PassReferenceWorkerTest {

    private final PassReferenceWorker worker = new PassReferenceWorker();

    @Test
    void taskDefName() {
        assertEquals("clc_pass_reference", worker.getTaskDefName());
    }

    @Test
    void passesClaimCheckIdThrough() {
        Task task = taskWith(Map.of(
                "claimCheckId", "CC-abc-123",
                "storageLocation", "s3://claim-checks/CC-abc-123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CC-abc-123", result.getOutputData().get("claimCheckId"));
    }

    @Test
    void passesStorageLocationThrough() {
        Task task = taskWith(Map.of(
                "claimCheckId", "CC-xyz",
                "storageLocation", "gcs://bucket/CC-xyz"));
        TaskResult result = worker.execute(task);

        assertEquals("gcs://bucket/CC-xyz", result.getOutputData().get("storageLocation"));
    }

    @Test
    void computesReferenceSizeBytes() {
        Task task = taskWith(Map.of(
                "claimCheckId", "CC-12345",
                "storageLocation", "s3://bucket/CC-12345"));
        TaskResult result = worker.execute(task);

        int refSize = (int) result.getOutputData().get("referenceSizeBytes");
        assertEquals("CC-12345".length(), refSize);
    }

    @Test
    void handlesNullClaimCheckId() {
        Map<String, Object> input = new HashMap<>();
        input.put("claimCheckId", null);
        input.put("storageLocation", "s3://test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("claimCheckId"));
        assertEquals(0, result.getOutputData().get("referenceSizeBytes"));
    }

    @Test
    void handlesNullStorageLocation() {
        Map<String, Object> input = new HashMap<>();
        input.put("claimCheckId", "CC-test");
        input.put("storageLocation", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("storageLocation"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("claimCheckId"));
    }

    @Test
    void referenceSizeIsSmallComparedToPayload() {
        Task task = taskWith(Map.of(
                "claimCheckId", "CC-small",
                "storageLocation", "s3://claim-checks/CC-small"));
        TaskResult result = worker.execute(task);

        int refSize = (int) result.getOutputData().get("referenceSizeBytes");
        assertTrue(refSize < 100, "Reference should be lightweight");
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
