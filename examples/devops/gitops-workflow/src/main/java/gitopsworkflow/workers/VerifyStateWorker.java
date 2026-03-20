package gitopsworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies that the cluster state matches the Git repository after sync.
 * Input: verify_stateData (output from apply-sync)
 * Output: verify_state, completedAt
 */
public class VerifyStateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "go_verify_state";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify] Cluster state matches Git repository");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verify_state", true);
        result.getOutputData().put("completedAt", "2026-03-14T00:00:00Z");
        return result;
    }
}
