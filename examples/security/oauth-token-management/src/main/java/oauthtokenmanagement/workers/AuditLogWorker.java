package oauthtokenmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Logs token issuance for compliance.
 * Input: audit_logData
 * Output: audit_log, completedAt
 */
public class AuditLogWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "otm_audit_log";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [audit] Token issuance logged for compliance");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("audit_log", true);
        result.getOutputData().put("completedAt", "2024-01-15T10:30:00Z");
        return result;
    }
}
