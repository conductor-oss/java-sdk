package permissionsync.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SyncWorkerTest {
    private final SyncWorker worker = new SyncWorker();
    @Test void taskDefName() { assertEquals("pms_sync", worker.getTaskDefName()); }
    @Test void syncsPermissions() {
        TaskResult r = worker.execute(taskWith(Map.of("diffs", List.of(), "targets", List.of("db"))));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(6, r.getOutputData().get("syncedCount"));
    }
    @SuppressWarnings("unchecked")
    @Test void noFailures() {
        TaskResult r = worker.execute(taskWith(Map.of("diffs", List.of(), "targets", List.of())));
        Map<String, Object> results = (Map<String, Object>) r.getOutputData().get("syncResults");
        assertEquals(0, results.get("failed"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
