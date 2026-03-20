package compliancemonitoring.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ScanResourcesWorkerTest {
    @Test void taskDefName() { assertEquals("cpm_scan_resources", new ScanResourcesWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("framework","SOC2", "scope","production")));
        TaskResult r = new ScanResourcesWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(256, r.getOutputData().get("resourcesScanned"));
    }
}
