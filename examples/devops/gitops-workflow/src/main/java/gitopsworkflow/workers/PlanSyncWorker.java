package gitopsworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Plans the synchronization actions needed to reconcile drift.
 * Input: plan_syncData (output from detect-drift)
 * Output: plan_sync, processed, updates, creates
 */
public class PlanSyncWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "go_plan_sync";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [plan] Sync plan: 2 updates, 1 create");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("plan_sync", true);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("updates", 2);
        result.getOutputData().put("creates", 1);
        return result;
    }
}
