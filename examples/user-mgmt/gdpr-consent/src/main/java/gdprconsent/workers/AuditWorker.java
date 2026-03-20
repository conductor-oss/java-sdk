package gdprconsent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.UUID;

public class AuditWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gdc_audit";
    }

    @Override
    public TaskResult execute(Task task) {
        String auditId = "AUDIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("  [audit] Audit trail created -> " + auditId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("auditTrailId", auditId);
        result.getOutputData().put("immutable", true);
        return result;
    }
}
