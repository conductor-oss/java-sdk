package sagapaymentinventory.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for spi_refund_payment -- compensation worker that refunds a payment.
 *
 * This is called as part of the saga compensation when shipping fails.
 */
public class RefundPaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "spi_refund_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        String paymentId = (String) task.getInputData().getOrDefault("paymentId", "unknown");
        String orderId = (String) task.getInputData().getOrDefault("orderId", "unknown");

        System.out.println("  [spi_refund_payment] Refunding payment " + paymentId + " for order: " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("paymentId", paymentId);
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("refundStatus", "refunded");

        return result;
    }
}
