package failureworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Cleanup worker for the failure handler workflow.
 * Runs after the main workflow fails, performing cleanup operations.
 * Returns { cleaned: true }.
 */
public class CleanupWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fw_cleanup";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [fw_cleanup] Running cleanup after failure...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cleaned", true);
        result.getOutputData().put("message", "Cleanup completed successfully");
        return result;
    }
}
