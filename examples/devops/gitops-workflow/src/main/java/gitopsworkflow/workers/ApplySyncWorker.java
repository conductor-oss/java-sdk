package gitopsworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Applies the synchronization plan to bring cluster into desired state.
 * Input: apply_syncData (output from plan-sync)
 * Output: apply_sync, processed
 */
public class ApplySyncWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "go_apply_sync";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [apply] All resources synced to desired state");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("apply_sync", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
