package penetrationtesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScanVulnerabilitiesWorkerTest {

    private final ScanVulnerabilitiesWorker worker = new ScanVulnerabilitiesWorker();

    @Test
    void taskDefName() {
        assertEquals("pen_scan_vulnerabilities", worker.getTaskDefName());
    }

    @Test
    void scansWithReconData() {
        Task task = taskWith(Map.of("scan_vulnerabilitiesData", Map.of("reconnaissanceId", "R-1")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("scan_vulnerabilities"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void scansWithEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("scan_vulnerabilities"));
    }

    @Test
    void handlesNullScanData() {
        Map<String, Object> input = new HashMap<>();
        input.put("scan_vulnerabilitiesData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of("scan_vulnerabilitiesData", "data"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("scan_vulnerabilities"));
        assertTrue(result.getOutputData().containsKey("processed"));
    }

    @Test
    void processedIsTrue() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("scan_vulnerabilitiesData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void scanValueIsTrue() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("scan_vulnerabilities"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
