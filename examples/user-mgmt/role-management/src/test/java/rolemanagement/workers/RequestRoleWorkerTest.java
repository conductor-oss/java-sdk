package rolemanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RequestRoleWorkerTest {
    private final RequestRoleWorker worker = new RequestRoleWorker();
    @Test void taskDefName() { assertEquals("rom_request_role", worker.getTaskDefName()); }
    @Test void logsRequest() {
        TaskResult r = worker.execute(taskWith(Map.of("userId", "USR-1", "role", "admin", "requestedBy", "MGR-1")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("requestId").toString().startsWith("RR-"));
        assertEquals(true, r.getOutputData().get("logged"));
    }
    @Test void completesSuccessfully() {
        TaskResult r = worker.execute(taskWith(Map.of("userId", "USR-2", "role", "editor", "requestedBy", "MGR-2")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
