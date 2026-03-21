package geofencing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Evaluates geofence boundaries against device position.
 * Input: deviceId, latitude, longitude
 * Output: zoneStatus (inside/outside), zoneName, distanceFromBoundary
 */
public class EvaluateBoundariesWorker implements Worker {

    private static final double FENCE_LAT = 37.78;
    private static final double FENCE_LON = -122.42;
    private static final double RADIUS = 0.01;

    @Override
    public String getTaskDefName() {
        return "geo_evaluate_boundaries";
    }

    @Override
    public TaskResult execute(Task task) {
        double lat = toDouble(task.getInputData().get("latitude"), 0.0);
        double lon = toDouble(task.getInputData().get("longitude"), 0.0);

        double dist = Math.sqrt(Math.pow(lat - FENCE_LAT, 2) + Math.pow(lon - FENCE_LON, 2));
        boolean inside = dist <= RADIUS;
        String distStr = String.format("%.4f", dist);

        System.out.println("  [boundary] Distance: " + distStr + " — " + (inside ? "INSIDE" : "OUTSIDE") + " zone");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("zoneStatus", inside ? "inside" : "outside");
        result.getOutputData().put("zoneName", "HQ-Campus");
        result.getOutputData().put("distanceFromBoundary", distStr);
        return result;
    }

    private double toDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
