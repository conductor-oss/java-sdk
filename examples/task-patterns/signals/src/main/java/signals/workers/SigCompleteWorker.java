package signals.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Completes the order after the delivery signal is received.
 * Takes orderId, deliveredAt, and signature. Returns { done: true }.
 */
public class SigCompleteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sig_complete";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        String deliveredAt = (String) task.getInputData().get("deliveredAt");
        String signature = (String) task.getInputData().get("signature");

        if (orderId == null || orderId.isBlank()) {
            orderId = "unknown";
        }
        if (deliveredAt == null || deliveredAt.isBlank()) {
            deliveredAt = "N/A";
        }
        if (signature == null || signature.isBlank()) {
            signature = "N/A";
        }

        System.out.println("  [sig_complete] Completing order: " + orderId
                + " | delivered at: " + deliveredAt + " | signature: " + signature);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("deliveredAt", deliveredAt);
        result.getOutputData().put("signature", signature);
        result.getOutputData().put("done", true);
        return result;
    }
}
