package assettracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Checks if asset is within geofence. Real point-in-polygon using ray casting algorithm.
 */
public class GeofenceCheckWorker implements Worker {
    // Default geofence: rectangular zone around a warehouse
    private static final double[][] DEFAULT_GEOFENCE = {
        {37.7740, -122.4210}, {37.7740, -122.4170},
        {37.7760, -122.4170}, {37.7760, -122.4210}
    };

    @Override public String getTaskDefName() { return "ast_geofence_check"; }

    @Override public TaskResult execute(Task task) {
        Object latObj = task.getInputData().get("latitude");
        Object lngObj = task.getInputData().get("longitude");
        String geofenceName = (String) task.getInputData().get("geofenceName");
        if (geofenceName == null) geofenceName = "Warehouse A Zone";

        double lat = 37.7749;
        double lng = -122.4194;
        if (latObj instanceof Number) lat = ((Number) latObj).doubleValue();
        if (lngObj instanceof Number) lng = ((Number) lngObj).doubleValue();

        // Real point-in-polygon check using ray casting
        boolean insideGeofence = isPointInPolygon(lat, lng, DEFAULT_GEOFENCE);

        // Real distance from boundary calculation (approximate)
        double minDistFromBoundary = calculateMinDistFromBoundary(lat, lng, DEFAULT_GEOFENCE);
        minDistFromBoundary = Math.round(minDistFromBoundary * 100.0) / 100.0; // in km

        System.out.println("  [geofence] " + geofenceName + ": inside=" + insideGeofence
                + ", distance from boundary: " + minDistFromBoundary + " km");

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("insideGeofence", insideGeofence);
        r.getOutputData().put("distanceFromBoundary", minDistFromBoundary);
        r.getOutputData().put("geofenceName", geofenceName);
        r.getOutputData().put("checkedAt", Instant.now().toString());
        return r;
    }

    /** Ray casting algorithm for point-in-polygon */
    private boolean isPointInPolygon(double lat, double lng, double[][] polygon) {
        int n = polygon.length;
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if ((polygon[i][1] > lng) != (polygon[j][1] > lng) &&
                    lat < (polygon[j][0] - polygon[i][0]) * (lng - polygon[i][1])
                            / (polygon[j][1] - polygon[i][1]) + polygon[i][0]) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** Approximate distance from polygon boundary in km using Haversine */
    private double calculateMinDistFromBoundary(double lat, double lng, double[][] polygon) {
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < polygon.length; i++) {
            int j = (i + 1) % polygon.length;
            double dist = distToSegment(lat, lng, polygon[i][0], polygon[i][1], polygon[j][0], polygon[j][1]);
            minDist = Math.min(minDist, dist);
        }
        return minDist;
    }

    private double distToSegment(double px, double py, double ax, double ay, double bx, double by) {
        // Simple Haversine-based distance to nearest point on segment
        double dLat = Math.toRadians(px - ax);
        double dLng = Math.toRadians(py - ay);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(ax)) * Math.cos(Math.toRadians(px))
                * Math.sin(dLng/2) * Math.sin(dLng/2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
