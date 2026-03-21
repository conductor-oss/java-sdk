package permissionsync.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ScanSystemsWorkerTest {
    private final ScanSystemsWorker worker = new ScanSystemsWorker();
    @Test void taskDefName() { assertEquals("pms_scan_systems", worker.getTaskDefName()); }
    @Test void scansPermissions() {
        TaskResult r = worker.execute(taskWith(Map.of("source", "ldap", "targets", List.of("db"))));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("sourcePermissions"));
        assertNotNull(r.getOutputData().get("targetPermissions"));
    }
    @Test void includesScannedAt() {
        TaskResult r = worker.execute(taskWith(Map.of("source", "ldap", "targets", List.of())));
        assertNotNull(r.getOutputData().get("scannedAt"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
