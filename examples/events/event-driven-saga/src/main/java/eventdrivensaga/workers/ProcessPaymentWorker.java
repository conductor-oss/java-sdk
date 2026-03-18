package eventdrivensaga.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes payment for an order.
 * Input: orderId, amount
 * Output: paymentStatus ("success"), transactionId ("txn_fixed_001"), amount
 */
public class ProcessPaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ds_process_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        if (orderId == null) {
            orderId = "UNKNOWN";
        }

        Object amount = task.getInputData().get("amount");
        if (amount == null) {
            amount = 0;
        }

        System.out.println("  [ds_process_payment] Processing $" + amount + " for order " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("paymentStatus", "success");
        result.getOutputData().put("transactionId", "txn_fixed_001");
        result.getOutputData().put("amount", amount);
        return result;
    }
}
