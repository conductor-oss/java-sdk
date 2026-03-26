package contentmoderation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AutoCheckWorkerTest {
    private final AutoCheckWorker worker = new AutoCheckWorker();
    @Test void taskDefName() { assertEquals("mod_auto_check", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("confidence"));
        assertNotNull(r.getOutputData().get("flagReasons"));
        assertNotNull(r.getOutputData().get("toxicityScore"));
    }
}
