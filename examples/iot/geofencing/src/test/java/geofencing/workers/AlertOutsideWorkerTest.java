package geofencing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertOutsideWorkerTest {

    private final AlertOutsideWorker worker = new AlertOutsideWorker();

    @Test
    void taskDefName() {
        assertEquals("geo_alert_outside", worker.getTaskDefName());
    }

    @Test
    void returnsExitAlertType() {
        Task task = taskWith(Map.of("deviceId", "DEV-1", "zone", "HQ-Campus", "distance", "0.0500"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("exit", result.getOutputData().get("alertType"));
    }

    @Test
    void returnsAcknowledged() {
        Task task = taskWith(Map.of("deviceId", "DEV-2", "zone", "Warehouse", "distance", "0.1000"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void handlesVariousDistances() {
        Task task = taskWith(Map.of("deviceId", "DEV-3", "zone", "Lab", "distance", "1.2345"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingDistance() {
        Task task = taskWith(Map.of("deviceId", "DEV-4", "zone", "HQ-Campus"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("exit", result.getOutputData().get("alertType"));
    }

    @Test
    void handlesMissingDeviceId() {
        Task task = taskWith(Map.of("zone", "HQ-Campus", "distance", "0.05"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingZone() {
        Task task = taskWith(Map.of("deviceId", "DEV-5", "distance", "0.05"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("exit", result.getOutputData().get("alertType"));
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
