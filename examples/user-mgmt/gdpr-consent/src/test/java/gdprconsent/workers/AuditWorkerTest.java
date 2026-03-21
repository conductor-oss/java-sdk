package gdprconsent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditWorkerTest {

    private final AuditWorker worker = new AuditWorker();

    @Test
    void taskDefName() {
        assertEquals("gdc_audit", worker.getTaskDefName());
    }

    @Test
    void createsAuditTrail() {
        Task task = taskWith(Map.of("userId", "USR-123", "consentRecord", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().get("auditTrailId").toString().startsWith("AUDIT-"));
    }

    @Test
    void isImmutable() {
        Task task = taskWith(Map.of("userId", "USR-123", "consentRecord", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("immutable"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
