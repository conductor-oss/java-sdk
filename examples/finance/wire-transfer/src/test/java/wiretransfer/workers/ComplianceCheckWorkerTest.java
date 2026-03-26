package wiretransfer.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ComplianceCheckWorkerTest {
    private final ComplianceCheckWorker worker = new ComplianceCheckWorker();
    @Test void taskDefName() { assertEquals("wir_compliance_check", worker.getTaskDefName()); }
    @Test void clearsBelowThreshold() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("amount", 5000)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(false, r.getOutputData().get("ctrRequired"));
    }
    @Test void requiresCtrAboveThreshold() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("amount", 15000)));
        TaskResult r = worker.execute(t);
        assertEquals(true, r.getOutputData().get("ctrRequired"));
    }
}
