package edgecomputing.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ProcessEdgeWorkerTest {
    private final ProcessEdgeWorker worker = new ProcessEdgeWorker();
    @Test void taskDefName() { assertEquals("edg_process_edge", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("edgeResult"));
        assertEquals(12, r.getOutputData().get("objectsDetected"));
        assertNotNull(r.getOutputData().get("classifications"));
    }
}
