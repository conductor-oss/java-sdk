package usermigration.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TransformWorkerTest {
    private final TransformWorker w = new TransformWorker();
    @Test void taskDefName() { assertEquals("umg_transform", w.getTaskDefName()); }
    @Test void transforms() {
        TaskResult r = w.execute(t(Map.of("users", List.of(Map.of("id", 1)))));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(500, r.getOutputData().get("transformedCount"));
    }
    @Test void addsFields() {
        TaskResult r = w.execute(t(Map.of("users", List.of())));
        assertNotNull(r.getOutputData().get("fieldsAdded"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
