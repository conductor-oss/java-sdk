package distributedtransactions.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PreparePaymentWorker implements Worker {
    @Override public String getTaskDefName() { return "dtx_prepare_payment"; }
    @Override public TaskResult execute(Task task) {
        String pm = (String) task.getInputData().getOrDefault("paymentMethod", "unknown");
        System.out.println("  [payment] Authorized: " + pm);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("txId", "PTX-" + System.currentTimeMillis());
        r.getOutputData().put("prepared", true);
        return r;
    }
}
