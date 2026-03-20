package usermigration.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ExtractWorkerTest {
    private final ExtractWorker w = new ExtractWorker();
    @Test void taskDefName() { assertEquals("umg_extract", w.getTaskDefName()); }
    @Test void extracts() {
        TaskResult r = w.execute(t(Map.of("sourceDb", "mysql", "batchSize", 100)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(500, r.getOutputData().get("extractedCount"));
        assertNotNull(r.getOutputData().get("users"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
