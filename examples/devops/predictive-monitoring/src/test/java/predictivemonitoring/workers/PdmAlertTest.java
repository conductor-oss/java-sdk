package predictivemonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PdmAlertTest {

    private final PdmAlert worker = new PdmAlert();

    @Test
    void taskDefName() {
        assertEquals("pdm_alert", worker.getTaskDefName());
    }

    @Test
    void highLikelihoodSendsAlertWithCriticalSeverity() {
        Task task = taskWith("cpu_usage", 85.0, 92.1);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("alertSent"));
        assertEquals("critical", result.getOutputData().get("severity"));
        String msg = (String) result.getOutputData().get("message");
        assertTrue(msg.contains("cpu_usage"));
    }

    @Test
    void moderateLikelihoodSendsAlertWithWarningSeverity() {
        Task task = taskWith("memory_pct", 65.0, 80.0);

        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("alertSent"));
        assertEquals("warning", result.getOutputData().get("severity"));
    }

    @Test
    void lowLikelihoodDoesNotSendAlert() {
        Task task = taskWith("disk_io", 30.0, 55.0);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("alertSent"));
        assertEquals("warning", result.getOutputData().get("severity"));
    }

    @Test
    void exactlyFiftyDoesNotTriggerAlert() {
        Task task = taskWith("network_in", 50.0, 70.0);

        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("alertSent"));
    }

    @Test
    void exactlyEightyOneIsCritical() {
        Task task = taskWith("latency", 81.0, 95.0);

        TaskResult result = worker.execute(task);

        assertEquals("critical", result.getOutputData().get("severity"));
    }

    @Test
    void nullLikelihoodDefaultsToZeroNoAlert() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("metricName", "cpu");
        // breachLikelihood omitted — should default to 0
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("alertSent"));
    }

    @Test
    void messageContainsPredictedPeak() {
        Task task = taskWith("cpu_usage", 72.0, 88.5);

        TaskResult result = worker.execute(task);

        String msg = (String) result.getOutputData().get("message");
        assertTrue(msg.contains("88.5"));
    }

    private Task taskWith(String metricName, double breachLikelihood, double predictedPeak) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("metricName", metricName);
        input.put("breachLikelihood", breachLikelihood);
        input.put("predictedPeak", predictedPeak);
        task.setInputData(input);
        return task;
    }
}
