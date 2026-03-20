package complexeventprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Triggers an alert when anomalous patterns are detected.
 * Input: sequenceDetected, absenceDetected, timingViolation
 * Output: alerted (true), severity ("high")
 */
public class TriggerAlertWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cp_trigger_alert";
    }

    @Override
    public TaskResult execute(Task task) {
        Object sequenceDetected = task.getInputData().get("sequenceDetected");
        Object absenceDetected = task.getInputData().get("absenceDetected");
        Object timingViolation = task.getInputData().get("timingViolation");

        System.out.println("  [cp_trigger_alert] PATTERN ALERT -- seq:" + sequenceDetected
                + ", absence:" + absenceDetected + ", timing:" + timingViolation);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("alerted", true);
        result.getOutputData().put("severity", "high");
        return result;
    }
}
