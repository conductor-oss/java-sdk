package geofencing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvaluateBoundariesWorkerTest {

    private final EvaluateBoundariesWorker worker = new EvaluateBoundariesWorker();

    @Test
    void taskDefName() {
        assertEquals("geo_evaluate_boundaries", worker.getTaskDefName());
    }

    @Test
    void deviceInsideZone() {
        Task task = taskWith(Map.of("deviceId", "DEV-1", "latitude", 37.78, "longitude", -122.42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("inside", result.getOutputData().get("zoneStatus"));
        assertEquals("HQ-Campus", result.getOutputData().get("zoneName"));
    }

    @Test
    void deviceOutsideZone() {
        Task task = taskWith(Map.of("deviceId", "DEV-2", "latitude", 37.80, "longitude", -122.45));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("outside", result.getOutputData().get("zoneStatus"));
    }

    @Test
    void returnsDistanceFromBoundary() {
        Task task = taskWith(Map.of("deviceId", "DEV-3", "latitude", 37.78, "longitude", -122.42));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("distanceFromBoundary"));
        String dist = (String) result.getOutputData().get("distanceFromBoundary");
        assertEquals("0.0000", dist);
    }

    @Test
    void deviceExactlyOnBoundary() {
        // radius = 0.01, place device at exactly radius distance
        double lat = 37.78 + 0.01;
        Task task = taskWith(Map.of("deviceId", "DEV-4", "latitude", lat, "longitude", -122.42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("inside", result.getOutputData().get("zoneStatus"));
    }

    @Test
    void deviceJustOutsideBoundary() {
        double lat = 37.78 + 0.02;
        Task task = taskWith(Map.of("deviceId", "DEV-5", "latitude", lat, "longitude", -122.42));
        TaskResult result = worker.execute(task);

        assertEquals("outside", result.getOutputData().get("zoneStatus"));
    }

    @Test
    void handlesStringCoordinates() {
        Task task = taskWith(Map.of("deviceId", "DEV-6", "latitude", "37.78", "longitude", "-122.42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("inside", result.getOutputData().get("zoneStatus"));
    }

    @Test
    void handlesNullCoordinates() {
        Map<String, Object> input = new HashMap<>();
        input.put("deviceId", "DEV-7");
        input.put("latitude", null);
        input.put("longitude", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // (0,0) is far from (37.78, -122.42), should be outside
        assertEquals("outside", result.getOutputData().get("zoneStatus"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
