package geofencing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks device location and normalizes coordinates.
 * Input: deviceId, latitude, longitude
 * Output: latitude, longitude, timestamp
 */
public class CheckLocationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "geo_check_location";
    }

    @Override
    public TaskResult execute(Task task) {
        Object latObj = task.getInputData().get("latitude");
        Object lonObj = task.getInputData().get("longitude");
        double lat = toDouble(latObj, 37.7899);
        double lon = toDouble(lonObj, -122.4194);

        System.out.println("  [location] Device " + task.getInputData().get("deviceId")
                + " at (" + lat + ", " + lon + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("latitude", lat);
        result.getOutputData().put("longitude", lon);
        result.getOutputData().put("timestamp", 1700000000000L);
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
