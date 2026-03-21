package privilegedaccess.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Automatically revokes privileged access after expiry.
 * Input: revoke_accessData (from grant step)
 * Output: revoke_access, completedAt
 */
public class PamRevokeAccessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pam_revoke_access";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [revoke] Privileged access automatically revoked after expiry");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("revoke_access", true);
        result.getOutputData().put("completedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
