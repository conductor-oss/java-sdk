package mobileapprovalflutter.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for mob_finalize -- finalizes the mobile approval after
 * the WAIT task receives the mobile response.
 *
 * Returns { done: true }.
 */
public class MobFinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mob_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [mob_finalize] Finalizing mobile approval...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("done", true);
        return result;
    }
}
