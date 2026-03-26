package nestedsubworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Charges payment for an order.
 * Returns a transaction ID and charged status.
 * Used by Level 2 sub-workflow (nested_payment).
 */
public class ChargeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nest_charge";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        Object amountObj = task.getInputData().get("amount");

        if (orderId == null || orderId.isBlank()) {
            orderId = "UNKNOWN";
        }

        double amount = 0.0;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).doubleValue();
        }

        System.out.println("  [nest_charge] Charging $" + amount + " for order " + orderId);

        String transactionId = "TXN-" + orderId;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transactionId", transactionId);
        result.getOutputData().put("charged", true);
        return result;
    }
}
