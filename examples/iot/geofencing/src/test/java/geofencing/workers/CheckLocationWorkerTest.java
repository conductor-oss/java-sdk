package geofencing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckLocationWorkerTest {

    private final CheckLocationWorker worker = new CheckLocationWorker();

    @Test
    void taskDefName() {
        assertEquals("geo_check_location", worker.getTaskDefName());
    }

    @Test
    void returnsProvidedCoordinates() {
        Task task = taskWith(Map.of("deviceId", "DEV-1", "latitude", 40.0, "longitude", -74.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(40.0, result.getOutputData().get("latitude"));
        assertEquals(-74.0, result.getOutputData().get("longitude"));
    }

    @Test
    void returnsTimestamp() {
        Task task = taskWith(Map.of("deviceId", "DEV-2", "latitude", 37.78, "longitude", -122.42));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("timestamp"));
    }

    @Test
    void handlesStringCoordinates() {
        Task task = taskWith(Map.of("deviceId", "DEV-3", "latitude", "51.5074", "longitude", "-0.1278"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(51.5074, result.getOutputData().get("latitude"));
        assertEquals(-0.1278, result.getOutputData().get("longitude"));
    }

    @Test
    void handlesNullLatitude() {
        Map<String, Object> input = new HashMap<>();
        input.put("deviceId", "DEV-4");
        input.put("latitude", null);
        input.put("longitude", -122.42);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(37.7899, result.getOutputData().get("latitude"));
    }

    @Test
    void handlesNullLongitude() {
        Map<String, Object> input = new HashMap<>();
        input.put("deviceId", "DEV-5");
        input.put("latitude", 37.78);
        input.put("longitude", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(-122.4194, result.getOutputData().get("longitude"));
    }

    @Test
    void handlesMissingCoordinates() {
        Task task = taskWith(Map.of("deviceId", "DEV-6"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(37.7899, result.getOutputData().get("latitude"));
        assertEquals(-122.4194, result.getOutputData().get("longitude"));
    }

    @Test
    void handlesInvalidStringCoordinate() {
        Task task = taskWith(Map.of("deviceId", "DEV-7", "latitude", "not-a-number", "longitude", -122.42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(37.7899, result.getOutputData().get("latitude"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
