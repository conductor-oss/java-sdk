package gdprdatadeletion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateAuditLogWorkerTest {

    private final GenerateAuditLogWorker worker = new GenerateAuditLogWorker();

    @Test
    void taskDefName() {
        assertEquals("gr_generate_audit_log", worker.getTaskDefName());
    }

    @Test
    void generatesAuditIdFromRequestId() {
        Task task = taskWith(Map.of("requestId", "GDPR-REQ-001",
                "deletedRecords", List.of(Map.of("recordId", "R1"))));
        TaskResult result = worker.execute(task);
        assertEquals("AUDIT-GDPR-REQ-001", result.getOutputData().get("auditId"));
    }

    @Test
    void statusCompletedWhenRecordsDeleted() {
        Task task = taskWith(Map.of("requestId", "REQ-001",
                "deletedRecords", List.of(Map.of("recordId", "R1"))));
        TaskResult result = worker.execute(task);
        assertEquals("COMPLETED", result.getOutputData().get("status"));
    }

    @Test
    void statusNoActionWhenNoRecordsDeleted() {
        Task task = taskWith(Map.of("requestId", "REQ-001",
                "deletedRecords", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals("NO_ACTION", result.getOutputData().get("status"));
    }

    @Test
    void returnsComplianceStandard() {
        Task task = taskWith(Map.of("requestId", "REQ-001",
                "deletedRecords", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals("GDPR Art. 17", result.getOutputData().get("complianceStandard"));
    }

    @Test
    void returnsRetentionDays() {
        Task task = taskWith(Map.of("requestId", "REQ-001",
                "deletedRecords", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(2555, result.getOutputData().get("retentionDays"));
    }

    @Test
    void handlesDefaultRequestId() {
        Task task = taskWith(Map.of("deletedRecords", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals("AUDIT-REQ-unknown", result.getOutputData().get("auditId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
