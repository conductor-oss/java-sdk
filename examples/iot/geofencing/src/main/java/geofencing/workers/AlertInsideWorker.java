package geofencing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles alert when device is inside the geofence zone.
 * Input: deviceId, zone
 * Output: alertType, acknowledged
 */
public class AlertInsideWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "geo_alert_inside";
    }

    @Override
    public TaskResult execute(Task task) {
        String deviceId = (String) task.getInputData().getOrDefault("deviceId", "unknown");
        String zone = (String) task.getInputData().getOrDefault("zone", "unknown");

        System.out.println("  [alert] Device " + deviceId + " is INSIDE zone " + zone);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("alertType", "entry");
        result.getOutputData().put("acknowledged", true);
        return result;
    }
}
