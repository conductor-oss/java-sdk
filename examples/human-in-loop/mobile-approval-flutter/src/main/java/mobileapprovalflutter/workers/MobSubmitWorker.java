package mobileapprovalflutter.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for mob_submit -- submits a mobile approval request.
 *
 * Returns { submitted: true }.
 */
public class MobSubmitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mob_submit";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [mob_submit] Submitting mobile approval request...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("submitted", true);
        return result;
    }
}
