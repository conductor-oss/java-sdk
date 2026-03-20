package sensordataprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Triggers alerts based on detected anomalies.
 * Input: batchId, sensorGroupId, anomalies, trend
 * Output: alertsTriggered, notifications, escalationLevel
 */
public class TriggerAlertsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sen_trigger_alerts";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<?> anomalies = (List<?>) task.getInputData().get("anomalies");
        if (anomalies == null) {
            anomalies = List.of();
        }

        String trend = (String) task.getInputData().get("trend");
        if (trend == null) {
            trend = "stable";
        }

        int alertsTriggered = anomalies.size();

        System.out.println("  [alert] Trend: " + trend + ", Alerts triggered: " + alertsTriggered);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("alertsTriggered", alertsTriggered);
        result.getOutputData().put("notifications",
                alertsTriggered > 0
                        ? List.of("email:ops@example.com", "sms:+1555000111")
                        : List.of());
        result.getOutputData().put("escalationLevel",
                alertsTriggered > 0 ? "warning" : "none");
        return result;
    }
}
