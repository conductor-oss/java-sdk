package gdprdatadeletion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Generates a GDPR-compliant audit log for the deletion request.
 * Input: requestId, userId, deletedRecords, deletionTimestamp
 * Output: auditId, status, retentionDays, complianceStandard
 */
public class GenerateAuditLogWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gr_generate_audit_log";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String requestId = (String) task.getInputData().getOrDefault("requestId", "REQ-unknown");
        List<Map<String, Object>> deletedRecords =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("deletedRecords", List.of());

        String auditId = "AUDIT-" + requestId;
        String status = deletedRecords.isEmpty() ? "NO_ACTION" : "COMPLETED";

        System.out.println("  [audit] Generated audit log " + auditId + ": "
                + deletedRecords.size() + " records erased, status: " + status);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("auditId", auditId);
        result.getOutputData().put("status", status);
        result.getOutputData().put("retentionDays", 2555);
        result.getOutputData().put("complianceStandard", "GDPR Art. 17");
        return result;
    }
}
