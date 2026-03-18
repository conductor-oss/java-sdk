package subworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Validates payment details.
 * Checks that the payment method and amount are present and valid.
 */
public class ValidatePaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sub_validate_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        String paymentMethod = (String) task.getInputData().get("paymentMethod");
        Object amountObj = task.getInputData().get("amount");

        boolean valid = true;
        String reason = "Payment validated";

        if (paymentMethod == null || paymentMethod.isBlank()) {
            valid = false;
            reason = "Missing payment method";
        } else if (amountObj == null) {
            valid = false;
            reason = "Missing amount";
        } else {
            double amount = toDouble(amountObj);
            if (amount <= 0) {
                valid = false;
                reason = "Invalid amount: " + amount;
            }
        }

        System.out.println("  [sub_validate_payment] " + reason
                + " (method=" + paymentMethod + ", amount=" + amountObj + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", valid);
        result.getOutputData().put("reason", reason);
        return result;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}
