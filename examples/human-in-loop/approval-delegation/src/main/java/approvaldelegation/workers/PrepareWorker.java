package approvaldelegation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for ad_prepare — prepares approval request data.
 *
 * Returns { ready: true } to indicate the workflow is ready
 * to pause at the WAIT task for initial approval.
 */
public class PrepareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ad_prepare";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [ad_prepare] Preparing approval request...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ready", true);

        return result;
    }
}
