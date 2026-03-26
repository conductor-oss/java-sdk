package rightsmanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class VerifyUsageWorkerTest {
    private final VerifyUsageWorker worker = new VerifyUsageWorker();
    @Test void taskDefName() { assertEquals("rts_verify_usage", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1", "territory", "US")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("territory"));
        assertEquals(false, r.getOutputData().get("territoryRestriction"));
        assertEquals("2026-03-08T10:00:00Z", r.getOutputData().get("verifiedAt"));
    }
}
