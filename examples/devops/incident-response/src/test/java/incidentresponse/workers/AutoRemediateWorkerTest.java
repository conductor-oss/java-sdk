package incidentresponse.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AutoRemediateWorkerTest {

    private final AutoRemediateWorker worker = new AutoRemediateWorker();

    @Test
    void taskDefName() {
        assertEquals("ir_auto_remediate", worker.getTaskDefName());
    }

    @Test
    void performsSystemStatusCheckWithEmptyDiagnostics() {
        Task task = taskWith(Map.of("diagnostics", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("remediated"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) result.getOutputData().get("actions");
        assertNotNull(actions);
        assertFalse(actions.isEmpty(), "Should have at least one action (system status check)");
    }

    @Test
    void identifiesHighCpuProcesses() {
        // Provide diagnostics with high CPU load
        Map<String, Object> diagnostics = new HashMap<>();
        diagnostics.put("cpuLoad", 5.0);
        diagnostics.put("service", "api-gateway");

        Task task = taskWith(Map.of("diagnostics", diagnostics));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("remediated"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) result.getOutputData().get("actions");
        assertTrue(actions.stream().anyMatch(a -> "identify-cpu-hogs".equals(a.get("action"))),
                "Should identify CPU-consuming processes");
    }

    @Test
    void checksServiceStatusWhenProvided() {
        Map<String, Object> diagnostics = new HashMap<>();
        diagnostics.put("service", "nonexistent-test-service");

        Task task = taskWith(Map.of("diagnostics", diagnostics));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) result.getOutputData().get("actions");
        assertTrue(actions.stream().anyMatch(a -> "check-service-status".equals(a.get("action"))),
                "Should check service status");
    }

    @Test
    void outputContainsRecommendations() {
        Task task = taskWith(Map.of("diagnostics", Map.of()));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("recommendations"));
    }

    @Test
    void outputContainsActionsCount() {
        Task task = taskWith(Map.of("diagnostics", Map.of()));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("actionsCount"));
        assertTrue(((Number) result.getOutputData().get("actionsCount")).intValue() >= 0);
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("remediated"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
