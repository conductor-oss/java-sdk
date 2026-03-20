package geofencing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles alert when device is outside the geofence zone.
 * Input: deviceId, zone, distance
 * Output: alertType, acknowledged
 */
public class AlertOutsideWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "geo_alert_outside";
    }

    @Override
    public TaskResult execute(Task task) {
        String deviceId = (String) task.getInputData().getOrDefault("deviceId", "unknown");
        String zone = (String) task.getInputData().getOrDefault("zone", "unknown");
        Object distance = task.getInputData().getOrDefault("distance", "0");

        System.out.println("  [alert] Device " + deviceId + " is OUTSIDE zone " + zone + " by " + distance);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("alertType", "exit");
        result.getOutputData().put("acknowledged", true);
        return result;
    }
}
