package usermigration.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyMigrationWorkerTest {
    private final VerifyMigrationWorker w = new VerifyMigrationWorker();
    @Test void taskDefName() { assertEquals("umg_verify", w.getTaskDefName()); }
    @Test void matchingCounts() {
        TaskResult r = w.execute(t(Map.of("loaded", 500, "expected", 500)));
        assertEquals(true, r.getOutputData().get("allMatch"));
    }
    @Test void mismatchingCounts() {
        TaskResult r = w.execute(t(Map.of("loaded", 499, "expected", 500)));
        assertEquals(false, r.getOutputData().get("allMatch"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
