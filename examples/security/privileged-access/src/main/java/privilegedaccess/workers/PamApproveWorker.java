package privilegedaccess.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Approves the privileged access request after security review.
 * Input: approveData (from request step)
 * Output: approve, processed
 */
public class PamApproveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pam_approve";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [approve] Security team approved -- risk: low");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approve", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
