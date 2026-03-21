package distributedtransactions.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PrepareInventoryWorker implements Worker {
    @Override public String getTaskDefName() { return "dtx_prepare_inventory"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [inventory] Reserved items");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("txId", "ITX-" + System.currentTimeMillis());
        r.getOutputData().put("prepared", true);
        return r;
    }
}
