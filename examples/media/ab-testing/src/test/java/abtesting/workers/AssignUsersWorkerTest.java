package abtesting.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AssignUsersWorkerTest {
    private final AssignUsersWorker worker = new AssignUsersWorker();
    @Test void taskDefName() { assertEquals("abt_assign_users", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("groupASize"));
        assertNotNull(r.getOutputData().get("groupBSize"));
        assertEquals("random_hash", r.getOutputData().get("assignmentMethod"));
    }
}
