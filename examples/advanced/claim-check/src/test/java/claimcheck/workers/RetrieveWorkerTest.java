package claimcheck.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetrieveWorkerTest {

    private final RetrieveWorker worker = new RetrieveWorker();

    @Test
    void taskDefName() {
        assertEquals("clc_retrieve", worker.getTaskDefName());
    }

    @Test
    void retrievesPayloadSuccessfully() {
        Task task = taskWith(Map.of(
                "claimCheckId", "CC-test-001",
                "storageLocation", "s3://claim-checks/CC-test-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue((boolean) result.getOutputData().get("retrieved"));
        assertNotNull(result.getOutputData().get("payload"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void payloadContainsReportId() {
        Task task = taskWith(Map.of(
                "claimCheckId", "CC-001",
                "storageLocation", "s3://bucket/CC-001"));
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("payload");
        assertEquals("RPT-2024-001", payload.get("reportId"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void payloadContainsThreeMetrics() {
        Task task = taskWith(Map.of(
                "claimCheckId", "CC-002",
                "storageLocation", "s3://bucket/CC-002"));
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("payload");
        List<Map<String, Object>> data = (List<Map<String, Object>>) payload.get("data");
        assertEquals(3, data.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void payloadContainsGeneratedAtTimestamp() {
        Task task = taskWith(Map.of(
                "claimCheckId", "CC-003",
                "storageLocation", "s3://bucket/CC-003"));
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("payload");
        assertEquals("2024-01-15T10:30:00Z", payload.get("generatedAt"));
    }

    @Test
    void handlesNullStorageLocation() {
        Map<String, Object> input = new HashMap<>();
        input.put("claimCheckId", "CC-004");
        input.put("storageLocation", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue((boolean) result.getOutputData().get("retrieved"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("payload"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void metricsContainExpectedNames() {
        Task task = taskWith(Map.of(
                "claimCheckId", "CC-005",
                "storageLocation", "s3://bucket/CC-005"));
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("payload");
        List<Map<String, Object>> data = (List<Map<String, Object>>) payload.get("data");
        List<String> metricNames = data.stream().map(m -> (String) m.get("metric")).toList();
        assertTrue(metricNames.contains("cpu_usage"));
        assertTrue(metricNames.contains("memory_usage"));
        assertTrue(metricNames.contains("disk_io"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
