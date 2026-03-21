package securityincident.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Remediates a security incident by applying fixes.
 * Input: remediateData (from investigate output)
 * Output: remediate, completedAt
 */
public class RemediateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "si_remediate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [remediate] API key revoked, access logs preserved, patches applied");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("remediate", true);
        result.getOutputData().put("completedAt", "2026-01-15T10:05:00Z");
        return result;
    }
}
