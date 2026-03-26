package geofencing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertInsideWorkerTest {

    private final AlertInsideWorker worker = new AlertInsideWorker();

    @Test
    void taskDefName() {
        assertEquals("geo_alert_inside", worker.getTaskDefName());
    }

    @Test
    void returnsEntryAlertType() {
        Task task = taskWith(Map.of("deviceId", "DEV-1", "zone", "HQ-Campus"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("entry", result.getOutputData().get("alertType"));
    }

    @Test
    void returnsAcknowledged() {
        Task task = taskWith(Map.of("deviceId", "DEV-2", "zone", "Warehouse"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void handlesVariousDeviceIds() {
        Task task = taskWith(Map.of("deviceId", "SENSOR-99", "zone", "Lab-B"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("entry", result.getOutputData().get("alertType"));
    }

    @Test
    void handlesMissingDeviceId() {
        Task task = taskWith(Map.of("zone", "HQ-Campus"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingZone() {
        Task task = taskWith(Map.of("deviceId", "DEV-3"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("entry", result.getOutputData().get("alertType"));
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of("deviceId", "DEV-5", "zone", "Parking"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("alertType"));
        assertTrue(result.getOutputData().containsKey("acknowledged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
