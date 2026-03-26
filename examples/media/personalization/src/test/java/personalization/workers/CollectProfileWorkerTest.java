package personalization.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CollectProfileWorkerTest {
    private final CollectProfileWorker worker = new CollectProfileWorker();
    @Test void taskDefName() { assertEquals("per_collect_profile", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("interests"));
        assertNotNull(r.getOutputData().get("demographics"));
        assertEquals("US-West", r.getOutputData().get("region"));
    }
}
