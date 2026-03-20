package edgecomputing.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class OffloadTaskWorkerTest {
    private final OffloadTaskWorker worker = new OffloadTaskWorker();
    @Test void taskDefName() { assertEquals("edg_offload_task", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("EDGE-JOB-534-001", r.getOutputData().get("edgeJobId"));
        assertEquals("2026-03-08T10:00:00Z", r.getOutputData().get("scheduledAt"));
        assertEquals(500, r.getOutputData().get("estimatedCompletionMs"));
    }
}
