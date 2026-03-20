package distributedtransactions.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PrepareOrderWorker implements Worker {
    @Override public String getTaskDefName() { return "dtx_prepare_order"; }
    @Override public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().getOrDefault("orderId", "unknown");
        System.out.println("  [order] Prepared: " + orderId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("txId", "OTX-" + System.currentTimeMillis());
        r.getOutputData().put("prepared", true);
        return r;
    }
}
