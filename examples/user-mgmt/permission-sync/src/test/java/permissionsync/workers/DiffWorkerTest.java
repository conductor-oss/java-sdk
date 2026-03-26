package permissionsync.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DiffWorkerTest {
    private final DiffWorker worker = new DiffWorker();
    @Test void taskDefName() { assertEquals("pms_diff", worker.getTaskDefName()); }
    @Test void findsDiffs() {
        TaskResult r = worker.execute(taskWith(Map.of("sourcePerms", Map.of(), "targetPerms", Map.of())));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(6, r.getOutputData().get("totalDiffs"));
    }
    @Test void includesDiffList() {
        TaskResult r = worker.execute(taskWith(Map.of("sourcePerms", Map.of(), "targetPerms", Map.of())));
        assertNotNull(r.getOutputData().get("diffs"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
