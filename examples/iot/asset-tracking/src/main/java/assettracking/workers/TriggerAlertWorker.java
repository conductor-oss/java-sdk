package assettracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Triggers alerts based on geofence status. Real alert routing logic.
 */
public class TriggerAlertWorker implements Worker {
    @Override public String getTaskDefName() { return "ast_trigger_alert"; }
    @Override public TaskResult execute(Task task) {
        Object insideObj = task.getInputData().get("insideGeofence");
        Object distObj = task.getInputData().get("distanceFromBoundary");
        boolean inside = Boolean.TRUE.equals(insideObj);

        double distance = 0;
        if (distObj instanceof Number) distance = ((Number) distObj).doubleValue();

        String alertType;
        List<String> notifications = new ArrayList<>();

        if (!inside && distance > 1.0) {
            alertType = "geofence_breach_critical";
            notifications.add("email:security@example.com");
            notifications.add("sms:+1555000222");
            notifications.add("push:ops_team");
        } else if (!inside) {
            alertType = "geofence_breach";
            notifications.add("email:security@example.com");
            notifications.add("sms:+1555000222");
        } else if (distance < 0.1) {
            alertType = "geofence_boundary_warning";
            notifications.add("push:ops_team");
        } else {
            alertType = "status_normal";
        }

        String alertId = "ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        System.out.println("  [alert] " + alertType + " -> " + notifications.size() + " notifications");

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("alertType", alertType);
        r.getOutputData().put("notifications", notifications);
        r.getOutputData().put("alertId", alertId);
        return r;
    }
}
