package runbookautomation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadRunbookWorkerTest {

    private final LoadRunbookWorker worker = new LoadRunbookWorker();

    @Test
    void taskDefName() {
        assertEquals("ra_load_runbook", worker.getTaskDefName());
    }

    @Test
    void loadsRunbookWithIdAndVersion() {
        Task task = taskWith(Map.of("runbookName", "database-failover"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("runbookId"));
        assertTrue(result.getOutputData().get("runbookId").toString().startsWith("RB-"));
        assertNotNull(result.getOutputData().get("version"));
    }

    @Test
    void createsRunbookFileOnDisk() {
        Task task = taskWith(Map.of("runbookName", "test-runbook-" + System.currentTimeMillis()));
        TaskResult result = worker.execute(task);

        String runbookPath = (String) result.getOutputData().get("runbookPath");
        assertNotNull(runbookPath);
        assertTrue(Files.exists(Path.of(runbookPath)), "Runbook file should exist on disk");
    }

    @Test
    void returnsStepsForDatabaseRunbook() {
        Task task = taskWith(Map.of("runbookName", "database-recovery"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) result.getOutputData().get("steps");
        assertNotNull(steps);
        assertFalse(steps.isEmpty(), "Should have steps");

        // Each step should have name, command, and timeoutSeconds
        for (Map<String, Object> step : steps) {
            assertNotNull(step.get("name"));
            assertNotNull(step.get("command"));
            assertNotNull(step.get("timeoutSeconds"));
        }
    }

    @Test
    void returnsStepCount() {
        Task task = taskWith(Map.of("runbookName", "default-diagnostics"));
        TaskResult result = worker.execute(task);

        int stepCount = ((Number) result.getOutputData().get("stepCount")).intValue();
        assertTrue(stepCount > 0, "Should have at least one step");
    }

    @Test
    void defaultStepsContainRealCommands() {
        List<Map<String, Object>> steps = LoadRunbookWorker.getDefaultSteps("default-diagnostics");
        assertFalse(steps.isEmpty());

        for (Map<String, Object> step : steps) {
            String command = (String) step.get("command");
            assertFalse(command.isBlank(), "Command should not be blank");
            // Commands should be real shell commands, not demo
            assertFalse(command.contains("DEMO"), "Command should be real, not demo");
        }
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("runbookId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
