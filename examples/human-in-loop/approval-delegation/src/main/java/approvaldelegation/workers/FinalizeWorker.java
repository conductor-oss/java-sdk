package approvaldelegation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for ad_finalize — finalizes the approval after it has been
 * approved (either directly or via delegation).
 *
 * Returns { done: true } to indicate the approval process is complete.
 */
public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ad_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [ad_finalize] Finalizing approval...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("done", true);

        return result;
    }
}
