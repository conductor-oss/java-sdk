package personalization.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SegmentUserWorkerTest {
    private final SegmentUserWorker worker = new SegmentUserWorker();
    @Test void taskDefName() { assertEquals("per_segment_user", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("tech_professional", r.getOutputData().get("segment"));
        assertNotNull(r.getOutputData().get("subSegments"));
        assertNotNull(r.getOutputData().get("confidence"));
    }
}
