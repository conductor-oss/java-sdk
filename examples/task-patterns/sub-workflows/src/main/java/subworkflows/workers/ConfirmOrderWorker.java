package subworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Confirms the order after payment has been processed.
 * Returns the orderId, transactionId, and confirmation status.
 */
public class ConfirmOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sub_confirm_order";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        String transactionId = (String) task.getInputData().get("transactionId");

        if (orderId == null || orderId.isBlank()) {
            orderId = "UNKNOWN";
        }
        if (transactionId == null || transactionId.isBlank()) {
            transactionId = "NONE";
        }

        System.out.println("  [sub_confirm_order] Order " + orderId
                + " confirmed with transaction " + transactionId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("transactionId", transactionId);
        result.getOutputData().put("confirmed", true);
        return result;
    }
}
