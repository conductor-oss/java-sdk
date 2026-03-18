package subworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Charges payment and returns a transaction ID.
 * Returns a deterministic transactionId: "TXN-" + orderId.
 */
public class ChargePaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sub_charge_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        Object amountObj = task.getInputData().get("amount");
        String paymentMethod = (String) task.getInputData().get("paymentMethod");

        if (orderId == null || orderId.isBlank()) {
            orderId = "UNKNOWN";
        }

        double amount = toDouble(amountObj);
        String transactionId = "TXN-" + orderId;

        System.out.println("  [sub_charge_payment] Charged " + amount
                + " via " + paymentMethod + " -> " + transactionId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transactionId", transactionId);
        result.getOutputData().put("charged", true);
        result.getOutputData().put("amount", amount);
        return result;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}
