package compliancenonprofit.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AuditWorkerTest {
    @Test void testExecute() { AuditWorker w = new AuditWorker(); assertEquals("cnp_audit", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("organizationName", "Test", "fiscalYear", 2025));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus()); }
}
