package usermigration.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LoadWorkerTest {
    private final LoadWorker w = new LoadWorker();
    @Test void taskDefName() { assertEquals("umg_load", w.getTaskDefName()); }
    @Test void loads() {
        TaskResult r = w.execute(t(Map.of("targetDb", "postgres", "transformedUsers", "")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(500, r.getOutputData().get("loadedCount"));
        assertEquals(0, r.getOutputData().get("failedCount"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
