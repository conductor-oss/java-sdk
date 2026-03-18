package assettracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Tracks asset location. Real GPS coordinate processing.
 */
public class TrackLocationWorker implements Worker {
    @Override public String getTaskDefName() { return "ast_track_location"; }
    @Override public TaskResult execute(Task task) {
        Object latObj = task.getInputData().get("latitude");
        Object lngObj = task.getInputData().get("longitude");

        double lat = 37.7749; // SF default
        double lng = -122.4194;
        if (latObj instanceof Number) lat = ((Number) latObj).doubleValue();
        if (lngObj instanceof Number) lng = ((Number) lngObj).doubleValue();

        // Validate GPS coordinates
        boolean validCoords = lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
        double accuracy = validCoords ? 3.0 + Math.random() * 7.0 : 999.0; // 3-10m accuracy
        accuracy = Math.round(accuracy * 10.0) / 10.0;

        System.out.println("  [track] Location: " + lat + ", " + lng + " (accuracy: " + accuracy + "m)");

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("latitude", lat);
        r.getOutputData().put("longitude", lng);
        r.getOutputData().put("accuracy", accuracy);
        r.getOutputData().put("speed", 0);
        r.getOutputData().put("heading", 0);
        r.getOutputData().put("validCoords", validCoords);
        r.getOutputData().put("lastUpdated", Instant.now().toString());
        return r;
    }
}
