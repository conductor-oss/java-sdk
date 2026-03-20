package claimcheck.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StorePayloadWorkerTest {

    private final StorePayloadWorker worker = new StorePayloadWorker();

    @Test
    void taskDefName() {
        assertEquals("clc_store_payload", worker.getTaskDefName());
    }

    @Test
    void storesPayloadAndReturnsClaimCheck() {
        Task task = taskWith(Map.of(
                "payload", Map.of("reportId", "RPT-001", "data", "test"),
                "storageType", "s3"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CC-fixed-001", result.getOutputData().get("claimCheckId"));
        assertNotNull(result.getOutputData().get("storageLocation"));
        assertNotNull(result.getOutputData().get("payloadSizeBytes"));
    }

    @Test
    void storageLocationContainsClaimCheckId() {
        Task task = taskWith(Map.of(
                "payload", Map.of("key", "value"),
                "storageType", "s3"));
        TaskResult result = worker.execute(task);

        String location = (String) result.getOutputData().get("storageLocation");
        assertTrue(location.startsWith("s3://claim-checks/"));
        assertTrue(location.contains("CC-fixed-001"));
    }

    @Test
    void payloadSizeBytesIsPositive() {
        Task task = taskWith(Map.of(
                "payload", Map.of("data", "some content"),
                "storageType", "s3"));
        TaskResult result = worker.execute(task);

        int size = (int) result.getOutputData().get("payloadSizeBytes");
        assertTrue(size > 0);
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("payload", null);
        input.put("storageType", "s3");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("claimCheckId"));
    }

    @Test
    void handlesNullStorageType() {
        Map<String, Object> input = new HashMap<>();
        input.put("payload", Map.of("x", 1));
        input.put("storageType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String location = (String) result.getOutputData().get("storageLocation");
        assertTrue(location.startsWith("s3://"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("claimCheckId"));
    }

    @Test
    void usesCustomStorageType() {
        Task task = taskWith(Map.of(
                "payload", Map.of("a", 1),
                "storageType", "gcs"));
        TaskResult result = worker.execute(task);

        String location = (String) result.getOutputData().get("storageLocation");
        assertTrue(location.startsWith("gcs://"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
