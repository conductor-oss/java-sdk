package autonomousagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalReportWorkerTest {

    private final FinalReportWorker worker = new FinalReportWorker();

    @Test
    void taskDefName() {
        assertEquals("aa_final_report", worker.getTaskDefName());
    }

    @Test
    void returnsReportForMission() {
        Task task = taskWith(Map.of(
                "mission", "Set up production monitoring for the platform",
                "goal", "Build and deploy a monitoring dashboard with alerting capabilities",
                "totalSteps", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String report = (String) result.getOutputData().get("report");
        assertTrue(report.contains("Set up production monitoring for the platform"));
        assertTrue(report.contains("Build and deploy a monitoring dashboard with alerting capabilities"));
        assertTrue(report.contains("3 autonomous steps"));
        assertTrue(report.contains("All systems operational"));
    }

    @Test
    void returnsSuccess() {
        Task task = taskWith(Map.of(
                "mission", "Test mission",
                "goal", "Test goal",
                "totalSteps", 3));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void returnsFixedCompletionTime() {
        Task task = taskWith(Map.of(
                "mission", "Test mission",
                "goal", "Test goal",
                "totalSteps", 3));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("completionTime"));
    }

    @Test
    void outputContainsReportSuccessAndCompletionTime() {
        Task task = taskWith(Map.of("mission", "M", "goal", "G", "totalSteps", 3));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("report"));
        assertTrue(result.getOutputData().containsKey("success"));
        assertTrue(result.getOutputData().containsKey("completionTime"));
    }

    @Test
    void handlesNullMission() {
        Map<String, Object> input = new HashMap<>();
        input.put("mission", null);
        input.put("goal", "Test goal");
        input.put("totalSteps", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String report = (String) result.getOutputData().get("report");
        assertTrue(report.contains("Unknown mission"));
    }

    @Test
    void handlesNullGoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("mission", "Test mission");
        input.put("goal", null);
        input.put("totalSteps", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String report = (String) result.getOutputData().get("report");
        assertTrue(report.contains("Unknown goal"));
    }

    @Test
    void handlesMissingTotalSteps() {
        Task task = taskWith(Map.of("mission", "Test mission", "goal", "Test goal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String report = (String) result.getOutputData().get("report");
        assertTrue(report.contains("3 autonomous steps"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("report"));
        assertEquals(true, result.getOutputData().get("success"));
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("completionTime"));
    }

    @Test
    void reportIncludesTotalStepsFromInput() {
        Task task = taskWith(Map.of("mission", "Deploy", "goal", "Deploy app", "totalSteps", 5));
        TaskResult result = worker.execute(task);

        String report = (String) result.getOutputData().get("report");
        assertTrue(report.contains("5 autonomous steps"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
