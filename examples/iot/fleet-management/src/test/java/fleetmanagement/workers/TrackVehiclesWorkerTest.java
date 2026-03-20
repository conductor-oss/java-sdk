package fleetmanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class TrackVehiclesWorkerTest {
    private final TrackVehiclesWorker worker = new TrackVehiclesWorker();
    @Test void taskDefName() { assertEquals("flt_track_vehicles", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("availableVehicles"));
        assertEquals("VEH-001", r.getOutputData().get("id"));
        assertNotNull(r.getOutputData().get("lat"));
    }
}
