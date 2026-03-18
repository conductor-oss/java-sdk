package incidentresponse.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GatherDiagnosticsWorkerTest {

    private final GatherDiagnosticsWorker worker = new GatherDiagnosticsWorker();

    @Test
    void taskDefName() {
        assertEquals("ir_gather_diagnostics", worker.getTaskDefName());
    }

    @Test
    void returnsRealDiskUsage() {
        Task task = taskWith(Map.of("service", "api-gateway"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> diskUsage = (List<Map<String, Object>>) result.getOutputData().get("diskUsage");
        assertNotNull(diskUsage);
        assertFalse(diskUsage.isEmpty(), "Should detect at least one filesystem");

        // Verify real disk data
        Map<String, Object> firstDisk = diskUsage.get(0);
        assertNotNull(firstDisk.get("totalBytes"));
        assertNotNull(firstDisk.get("usedBytes"));
        assertNotNull(firstDisk.get("availableBytes"));
        assertTrue(((Number) firstDisk.get("totalBytes")).longValue() > 0, "Total disk space should be positive");
    }

    @Test
    void returnsRealMemoryUsage() {
        Task task = taskWith(Map.of("service", "api-gateway"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> memoryUsage = (Map<String, Object>) result.getOutputData().get("memoryUsage");
        assertNotNull(memoryUsage);
        assertNotNull(memoryUsage.get("heapUsedBytes"));
        assertNotNull(memoryUsage.get("heapMaxBytes"));
        assertTrue(((Number) memoryUsage.get("heapUsedBytes")).longValue() > 0,
                "Heap used should be positive");
    }

    @Test
    void returnsRealCpuLoad() {
        Task task = taskWith(Map.of("service", "api-gateway"));
        TaskResult result = worker.execute(task);

        Object cpuLoad = result.getOutputData().get("cpuLoad");
        assertNotNull(cpuLoad);
        assertInstanceOf(Number.class, cpuLoad);
        // On some systems load average can be -1 if not available
    }

    @Test
    void returnsRealUptime() {
        Task task = taskWith(Map.of("service", "api-gateway"));
        TaskResult result = worker.execute(task);

        String uptime = (String) result.getOutputData().get("uptime");
        assertNotNull(uptime);
        assertFalse(uptime.isBlank(), "Uptime string should not be blank");
    }

    @Test
    void passesServiceNameThrough() {
        Task task = taskWith(Map.of("service", "payment-service"));
        TaskResult result = worker.execute(task);

        assertEquals("payment-service", result.getOutputData().get("service"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("diskUsage"));
        assertNotNull(result.getOutputData().get("memoryUsage"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
