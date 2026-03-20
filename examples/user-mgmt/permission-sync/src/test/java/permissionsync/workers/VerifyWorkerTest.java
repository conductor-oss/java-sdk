package permissionsync.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyWorkerTest {
    private final VerifyWorker worker = new VerifyWorker();
    @Test void taskDefName() { assertEquals("pms_verify", worker.getTaskDefName()); }
    @Test void verifiesAll() {
        TaskResult r = worker.execute(taskWith(Map.of("syncResults", Map.of("success", 6, "failed", 0))));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("allVerified"));
    }
    @Test void includesVerifiedAt() {
        TaskResult r = worker.execute(taskWith(Map.of("syncResults", Map.of())));
        assertNotNull(r.getOutputData().get("verifiedAt"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
