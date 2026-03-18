package eventdrivensaga.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Cancels an order as part of saga compensation.
 * Input: orderId, reason
 * Output: cancelled (true), orderId
 */
public class CancelOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ds_cancel_order";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        if (orderId == null) {
            orderId = "UNKNOWN";
        }

        String reason = (String) task.getInputData().get("reason");
        if (reason == null) {
            reason = "no reason given";
        }

        System.out.println("  [ds_cancel_order] Cancelling order " + orderId + ": " + reason);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cancelled", true);
        result.getOutputData().put("orderId", orderId);
        return result;
    }
}
