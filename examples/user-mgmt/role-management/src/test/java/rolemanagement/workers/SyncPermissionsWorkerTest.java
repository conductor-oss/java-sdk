package rolemanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SyncPermissionsWorkerTest {
    private final SyncPermissionsWorker worker = new SyncPermissionsWorker();
    @Test void taskDefName() { assertEquals("rom_sync_permissions", worker.getTaskDefName()); }
    @Test void syncsPerms() {
        TaskResult r = worker.execute(taskWith(Map.of("userId", "USR-1", "role", "admin", "permissions", List.of("read"))));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("synced"));
    }
    @SuppressWarnings("unchecked")
    @Test void includesTargets() {
        TaskResult r = worker.execute(taskWith(Map.of("userId", "USR-1", "role", "admin", "permissions", List.of("read"))));
        List<String> targets = (List<String>) r.getOutputData().get("targets");
        assertEquals(3, targets.size());
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
