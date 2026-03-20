package complexeventprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TriggerAlertWorkerTest {

    private final TriggerAlertWorker worker = new TriggerAlertWorker();

    @Test
    void taskDefName() {
        assertEquals("cp_trigger_alert", worker.getTaskDefName());
    }

    @Test
    void triggersAlertWithAllTrue() {
        Task task = taskWith(Map.of(
                "sequenceDetected", true,
                "absenceDetected", true,
                "timingViolation", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("alerted"));
        assertEquals("high", result.getOutputData().get("severity"));
    }

    @Test
    void triggersAlertWithAllFalse() {
        Task task = taskWith(Map.of(
                "sequenceDetected", false,
                "absenceDetected", false,
                "timingViolation", false));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("alerted"));
        assertEquals("high", result.getOutputData().get("severity"));
    }

    @Test
    void triggersAlertWithMixedValues() {
        Task task = taskWith(Map.of(
                "sequenceDetected", true,
                "absenceDetected", false,
                "timingViolation", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("alerted"));
    }

    @Test
    void handlesNullSequenceDetected() {
        Map<String, Object> input = new HashMap<>();
        input.put("sequenceDetected", null);
        input.put("absenceDetected", true);
        input.put("timingViolation", false);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("alerted"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("alerted"));
        assertEquals("high", result.getOutputData().get("severity"));
    }

    @Test
    void alwaysReturnsSeverityHigh() {
        Task task = taskWith(Map.of(
                "sequenceDetected", false,
                "absenceDetected", false,
                "timingViolation", false));
        TaskResult result = worker.execute(task);

        assertEquals("high", result.getOutputData().get("severity"));
    }

    @Test
    void handlesStringInputValues() {
        Task task = taskWith(Map.of(
                "sequenceDetected", "true",
                "absenceDetected", "false",
                "timingViolation", "true"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("alerted"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
