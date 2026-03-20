package eventdrivenmicroservices.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes payment for an order.
 * Input: orderId, amount, paymentMethod
 * Output: transactionId ("TXN-fixed-001"), status ("charged"), amount
 */
public class PaymentServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dm_payment_service";
    }

    @Override
    public TaskResult execute(Task task) {
        Object amountRaw = task.getInputData().get("amount");
        Object amount = amountRaw != null ? amountRaw : 0;

        String paymentMethod = (String) task.getInputData().get("paymentMethod");
        if (paymentMethod == null) {
            paymentMethod = "unknown";
        }

        String transactionId = "TXN-fixed-001";

        System.out.println("  [payment-svc] Charged $" + amount
                + " via " + paymentMethod + " -> " + transactionId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transactionId", transactionId);
        result.getOutputData().put("status", "charged");
        result.getOutputData().put("amount", amount);
        return result;
    }
}
