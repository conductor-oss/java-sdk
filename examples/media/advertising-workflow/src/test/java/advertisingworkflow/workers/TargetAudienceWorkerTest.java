package advertisingworkflow.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class TargetAudienceWorkerTest {
    private final TargetAudienceWorker worker = new TargetAudienceWorker();
    @Test void taskDefName() { assertEquals("adv_target_audience", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2500000, r.getOutputData().get("audienceSize"));
        assertNotNull(r.getOutputData().get("segments"));
        assertNotNull(r.getOutputData().get("demographics"));
    }
}
