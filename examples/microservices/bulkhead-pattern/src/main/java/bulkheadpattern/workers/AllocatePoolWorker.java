package bulkheadpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Allocates a slot in the specified resource pool.
 * Input: pool, maxConcurrency
 * Output: poolId, allocated
 */
public class AllocatePoolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bh_allocate_pool";
    }

    @Override
    public TaskResult execute(Task task) {
        String pool = (String) task.getInputData().get("pool");
        Object maxConcurrency = task.getInputData().get("maxConcurrency");
        if (pool == null) pool = "default-pool";

        System.out.println("  [bh_allocate_pool] Allocated slot in " + pool + " (max: " + maxConcurrency + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("poolId", "POOL-" + pool.hashCode());
        result.getOutputData().put("allocated", true);
        return result;
    }
}
