package maintenancewindows.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ExecuteMaintenanceWorkerTest {
    @Test void taskDefName() { assertEquals("mnw_execute_maintenance", new ExecuteMaintenanceWorker().getTaskDefName()); }
    @Test void executesMaintenance() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("system", "prod-db", "maintenanceType", "db-opt", "windowEnd", "2026-03-08T06:00:00Z")));
        TaskResult r = new ExecuteMaintenanceWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("executed"));
    }
}
