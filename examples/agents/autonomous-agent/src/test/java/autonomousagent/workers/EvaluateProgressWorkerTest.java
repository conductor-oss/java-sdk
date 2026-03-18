package autonomousagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvaluateProgressWorkerTest {

    private final EvaluateProgressWorker worker = new EvaluateProgressWorker();

    @Test
    void taskDefName() {
        assertEquals("aa_evaluate_progress", worker.getTaskDefName());
    }

    @Test
    void iteration1Returns33Percent() {
        Task task = taskWith(Map.of(
                "stepResult", "Metrics pipeline deployed",
                "iteration", 1,
                "goal", "Build monitoring dashboard"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(33, result.getOutputData().get("progress"));
        assertEquals(true, result.getOutputData().get("onTrack"));
        assertEquals("Step 1 completed successfully, 66% remaining",
                result.getOutputData().get("assessment"));
    }

    @Test
    void iteration2Returns66Percent() {
        Task task = taskWith(Map.of(
                "stepResult", "Dashboard created",
                "iteration", 2,
                "goal", "Build monitoring dashboard"));
        TaskResult result = worker.execute(task);

        assertEquals(66, result.getOutputData().get("progress"));
        assertEquals("Step 2 completed successfully, 33% remaining",
                result.getOutputData().get("assessment"));
    }

    @Test
    void iteration3Returns100Percent() {
        Task task = taskWith(Map.of(
                "stepResult", "Alerts configured",
                "iteration", 3,
                "goal", "Build monitoring dashboard"));
        TaskResult result = worker.execute(task);

        assertEquals(100, result.getOutputData().get("progress"));
        assertEquals("Step 3 completed successfully, 0% remaining",
                result.getOutputData().get("assessment"));
    }

    @Test
    void outputContainsProgressOnTrackAndAssessment() {
        Task task = taskWith(Map.of("stepResult", "Done", "iteration", 1, "goal", "Goal"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("progress"));
        assertTrue(result.getOutputData().containsKey("onTrack"));
        assertTrue(result.getOutputData().containsKey("assessment"));
    }

    @Test
    void alwaysOnTrack() {
        for (int i = 1; i <= 3; i++) {
            Task task = taskWith(Map.of("stepResult", "Step result", "iteration", i, "goal", "Goal"));
            TaskResult result = worker.execute(task);
            assertEquals(true, result.getOutputData().get("onTrack"));
        }
    }

    @Test
    void handlesNullStepResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("stepResult", null);
        input.put("iteration", 1);
        input.put("goal", "Goal");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("assessment"));
    }

    @Test
    void handlesMissingIteration() {
        Task task = taskWith(Map.of("stepResult", "Done", "goal", "Goal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(33, result.getOutputData().get("progress"));
    }

    @Test
    void handlesStringIteration() {
        Task task = taskWith(Map.of("stepResult", "Done", "iteration", "3", "goal", "Goal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(100, result.getOutputData().get("progress"));
        assertEquals("Step 3 completed successfully, 0% remaining",
                result.getOutputData().get("assessment"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("assessment"));
        assertEquals(33, result.getOutputData().get("progress"));
        assertEquals(true, result.getOutputData().get("onTrack"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
