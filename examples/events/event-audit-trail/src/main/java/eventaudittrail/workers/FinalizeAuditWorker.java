package eventaudittrail.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Finalizes the audit trail.
 * Input: eventId, stages
 * Output: auditTrailId ("audit_<eventId>_fixed"), totalStages, finalized (true)
 */
public class FinalizeAuditWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "at_finalize_audit";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        Object stagesObj = task.getInputData().get("stages");
        int stages = 0;
        if (stagesObj instanceof Number) {
            stages = ((Number) stagesObj).intValue();
        }

        System.out.println("  [at_finalize_audit] Finalizing audit for event: " + eventId + " with " + stages + " stages");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("auditTrailId", "audit_" + eventId + "_fixed");
        result.getOutputData().put("totalStages", stages);
        result.getOutputData().put("finalized", true);
        return result;
    }
}
