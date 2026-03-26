package emailcampaign.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SegmentAudienceWorkerTest {
    private final SegmentAudienceWorker worker = new SegmentAudienceWorker();
    @Test void taskDefName() { assertEquals("eml_segment_audience", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("segments"));
        assertEquals(15000, r.getOutputData().get("recipientCount"));
        assertEquals(320, r.getOutputData().get("suppressedCount"));
    }
}
