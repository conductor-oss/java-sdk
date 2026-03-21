package rightsmanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CheckLicenseWorkerTest {
    private final CheckLicenseWorker worker = new CheckLicenseWorker();
    @Test void taskDefName() { assertEquals("rts_check_license", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("valid"));
        assertEquals("2027-12-31", r.getOutputData().get("expirationDate"));
        assertNotNull(r.getOutputData().get("allowedUsages"));
    }
}
