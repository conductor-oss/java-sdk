package eventaudittrail.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeAuditWorkerTest {

    private final FinalizeAuditWorker worker = new FinalizeAuditWorker();

    @Test
    void taskDefName() {
        assertEquals("at_finalize_audit", worker.getTaskDefName());
    }

    @Test
    void finalizesAuditTrail() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "stages", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("audit_evt-001_fixed", result.getOutputData().get("auditTrailId"));
        assertEquals(3, result.getOutputData().get("totalStages"));
        assertEquals(true, result.getOutputData().get("finalized"));
    }

    @Test
    void outputContainsAuditTrailId() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "stages", 5));
        TaskResult result = worker.execute(task);

        assertEquals("audit_evt-002_fixed", result.getOutputData().get("auditTrailId"));
    }

    @Test
    void outputContainsTotalStages() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "stages", 7));
        TaskResult result = worker.execute(task);

        assertEquals(7, result.getOutputData().get("totalStages"));
    }

    @Test
    void outputContainsFinalizedTrue() {
        Task task = taskWith(Map.of(
                "eventId", "evt-004",
                "stages", 3));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("finalized"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("stages", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("audit_unknown_fixed", result.getOutputData().get("auditTrailId"));
    }

    @Test
    void handlesNullStages() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-005");
        input.put("stages", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalStages"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("audit_unknown_fixed", result.getOutputData().get("auditTrailId"));
        assertEquals(0, result.getOutputData().get("totalStages"));
        assertEquals(true, result.getOutputData().get("finalized"));
    }

    @Test
    void handlesZeroStages() {
        Task task = taskWith(Map.of(
                "eventId", "evt-006",
                "stages", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalStages"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
