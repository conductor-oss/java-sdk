package usermigration.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NotifyMigrationWorkerTest {
    private final NotifyMigrationWorker w = new NotifyMigrationWorker();
    @Test void taskDefName() { assertEquals("umg_notify", w.getTaskDefName()); }
    @Test void notifies() {
        TaskResult r = w.execute(t(Map.of("migrationResult", Map.of())));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("notified"));
        assertEquals("slack", r.getOutputData().get("channel"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
