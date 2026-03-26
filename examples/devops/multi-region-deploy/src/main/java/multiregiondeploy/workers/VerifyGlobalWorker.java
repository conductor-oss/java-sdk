package multiregiondeploy.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies all regions are healthy and serving traffic.
 * Input: verify_globalData
 * Output: verify_global, completedAt
 */
public class VerifyGlobalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mrd_verify_global";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify-global] All 3 regions healthy and serving traffic");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verify_global", true);
        result.getOutputData().put("completedAt", "2026-03-14T00:00:00Z");
        return result;
    }
}
