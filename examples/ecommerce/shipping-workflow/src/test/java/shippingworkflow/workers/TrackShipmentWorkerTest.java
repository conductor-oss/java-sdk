package shippingworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TrackShipmentWorkerTest {

    private final TrackShipmentWorker worker = new TrackShipmentWorker();

    @Test
    void taskDefName() { assertEquals("shp_track", worker.getTaskDefName()); }

    @Test
    void returnsTrackingEvents() {
        Task task = taskWith(Map.of("trackingNumber", "FX123", "carrier", "FedEx"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("events") instanceof List);
        assertEquals("out_for_delivery", r.getOutputData().get("currentStatus"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
