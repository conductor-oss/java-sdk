package oauthtokenmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogWorkerTest {

    private final AuditLogWorker worker = new AuditLogWorker();

    @Test void taskDefName() { assertEquals("otm_audit_log", worker.getTaskDefName()); }

    @Test void logsAuditSuccessfully() {
        Task task = taskWith(Map.of("audit_logData", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("audit_log"));
        assertNotNull(result.getOutputData().get("completedAt"));
    }

    @Test void outputContainsAuditLog() {
        assertEquals(true, worker.execute(taskWith(Map.of())).getOutputData().get("audit_log"));
    }

    @Test void outputContainsCompletedAt() {
        assertNotNull(worker.execute(taskWith(Map.of())).getOutputData().get("completedAt"));
    }

    @Test void handlesMissingInputs() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of())).getStatus());
    }

    @Test void handlesExtraInputs() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of("extra", "val"))).getStatus());
    }

    @Test void deterministicOutput() {
        assertEquals(worker.execute(taskWith(Map.of())).getOutputData().get("audit_log"),
                     worker.execute(taskWith(Map.of())).getOutputData().get("audit_log"));
    }

    @Test void completedAtFormat() {
        String c = (String) worker.execute(taskWith(Map.of())).getOutputData().get("completedAt");
        assertTrue(c.contains("T"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input)); return task;
    }
}
