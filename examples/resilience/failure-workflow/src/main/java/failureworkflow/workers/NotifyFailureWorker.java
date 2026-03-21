package failureworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Notification worker for the failure handler workflow.
 * Sends a failure alert after cleanup is done.
 * Returns { notified: true }.
 */
public class NotifyFailureWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fw_notify_failure";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [fw_notify_failure] Sending failure notification...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        result.getOutputData().put("message", "Failure notification sent");
        return result;
    }
}
