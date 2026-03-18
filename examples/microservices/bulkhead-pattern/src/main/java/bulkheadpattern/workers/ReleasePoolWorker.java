package bulkheadpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Releases the pool slot after request execution.
 * Input: poolId
 * Output: released
 */
public class ReleasePoolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bh_release_pool";
    }

    @Override
    public TaskResult execute(Task task) {
        String poolId = (String) task.getInputData().get("poolId");
        if (poolId == null) poolId = "unknown";

        System.out.println("  [bh_release_pool] Released slot in " + poolId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("released", true);
        return result;
    }
}
