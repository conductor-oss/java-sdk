package assettracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Updates the asset registry with latest location and status.
 */
public class UpdateRegistryWorker implements Worker {
    @Override public String getTaskDefName() { return "ast_update_registry"; }
    @Override public TaskResult execute(Task task) {
        Object latObj = task.getInputData().get("latitude");
        Object lngObj = task.getInputData().get("longitude");
        Object insideObj = task.getInputData().get("insideGeofence");

        double lat = 0, lng = 0;
        if (latObj instanceof Number) lat = ((Number) latObj).doubleValue();
        if (lngObj instanceof Number) lng = ((Number) lngObj).doubleValue();

        Map<String, Object> location = new LinkedHashMap<>();
        location.put("lat", lat);
        location.put("lng", lng);

        String status = Boolean.TRUE.equals(insideObj) ? "in_zone" : "out_of_zone";

        System.out.println("  [update] Registry updated: " + lat + ", " + lng + " (" + status + ")");

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("updated", true);
        r.getOutputData().put("lastKnownLocation", location);
        r.getOutputData().put("status", status);
        r.getOutputData().put("updatedAt", Instant.now().toString());
        return r;
    }
}
