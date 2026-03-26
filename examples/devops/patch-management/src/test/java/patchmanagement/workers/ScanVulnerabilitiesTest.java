package patchmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScanVulnerabilitiesTest {

    private final ScanVulnerabilities worker = new ScanVulnerabilities();

    @Test
    void taskDefName() {
        assertEquals("pm_scan_vulnerabilities", worker.getTaskDefName());
    }

    @Test
    void scansCriticalSeverity() {
        Task task = taskWith("PATCH-2024-001", "critical");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SCAN-1352", result.getOutputData().get("scanId"));
        assertEquals("PATCH-2024-001", result.getOutputData().get("patchId"));
        assertEquals("critical", result.getOutputData().get("severity"));
        assertEquals(24, result.getOutputData().get("affectedHosts"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void scansHighSeverity() {
        Task task = taskWith("PATCH-002", "high");
        TaskResult result = worker.execute(task);

        assertEquals(16, result.getOutputData().get("affectedHosts"));
        assertEquals(32, result.getOutputData().get("vulnerabilityCount"));
    }

    @Test
    void scansMediumSeverity() {
        Task task = taskWith("PATCH-003", "medium");
        TaskResult result = worker.execute(task);

        assertEquals(8, result.getOutputData().get("affectedHosts"));
    }

    @Test
    void scansLowSeverity() {
        Task task = taskWith("PATCH-004", "low");
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("affectedHosts"));
    }

    @Test
    void handlesNullPatchId() {
        Map<String, Object> input = new HashMap<>();
        input.put("patchId", null);
        input.put("severity", "medium");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("PATCH-UNKNOWN", result.getOutputData().get("patchId"));
    }

    @Test
    void handlesNullSeverity() {
        Map<String, Object> input = new HashMap<>();
        input.put("patchId", "PATCH-005");
        input.put("severity", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("medium", result.getOutputData().get("severity"));
        assertEquals(8, result.getOutputData().get("affectedHosts"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void vulnerabilityCountIsDoubleHosts() {
        Task task = taskWith("PATCH-006", "critical");
        TaskResult result = worker.execute(task);

        int hosts = (int) result.getOutputData().get("affectedHosts");
        int vulns = (int) result.getOutputData().get("vulnerabilityCount");
        assertEquals(hosts * 2, vulns);
    }

    private Task taskWith(String patchId, String severity) {
        Map<String, Object> input = new HashMap<>();
        input.put("patchId", patchId);
        input.put("severity", severity);
        return taskWith(input);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
