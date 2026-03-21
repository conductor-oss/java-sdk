package eventchoreography.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes payment for an order.
 * Input: orderId, amount
 * Output: paid (true), transactionId ("txn_ch_fixed_001")
 */
public class PaymentServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ch_payment_service";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        Object amount = task.getInputData().get("amount");

        System.out.println("  [ch_payment_service] Charged " + amount + " for order " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("paid", true);
        result.getOutputData().put("transactionId", "txn_ch_fixed_001");
        return result;
    }
}
