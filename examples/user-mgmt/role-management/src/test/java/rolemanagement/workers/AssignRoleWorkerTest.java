package rolemanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AssignRoleWorkerTest {
    private final AssignRoleWorker worker = new AssignRoleWorker();
    @Test void taskDefName() { assertEquals("rom_assign", worker.getTaskDefName()); }
    @SuppressWarnings("unchecked")
    @Test void assignsAdminWith4Perms() {
        TaskResult r = worker.execute(taskWith(Map.of("userId", "USR-1", "role", "admin", "approved", true)));
        assertEquals(true, r.getOutputData().get("assigned"));
        assertEquals(4, ((List<String>) r.getOutputData().get("permissions")).size());
    }
    @SuppressWarnings("unchecked")
    @Test void assignsViewerWithReadOnly() {
        TaskResult r = worker.execute(taskWith(Map.of("userId", "USR-1", "role", "viewer", "approved", true)));
        List<String> perms = (List<String>) r.getOutputData().get("permissions");
        assertEquals(1, perms.size());
        assertEquals("read", perms.get(0));
    }
    @Test void unknownRoleDefaultsToRead() {
        TaskResult r = worker.execute(taskWith(Map.of("userId", "USR-1", "role", "unknown", "approved", true)));
        assertNotNull(r.getOutputData().get("permissions"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
