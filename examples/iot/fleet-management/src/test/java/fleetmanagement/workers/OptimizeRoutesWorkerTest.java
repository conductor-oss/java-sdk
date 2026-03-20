package fleetmanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class OptimizeRoutesWorkerTest {
    private final OptimizeRoutesWorker worker = new OptimizeRoutesWorker();
    @Test void taskDefName() { assertEquals("flt_optimize_routes", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("assignedVehicleId"));
        assertEquals("DRV-042", r.getOutputData().get("driverId"));
        assertEquals("RTE-535-001", r.getOutputData().get("routeId"));
    }
}
