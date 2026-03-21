package sagapaymentinventory.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for spi_charge_payment -- charges payment for an order.
 *
 * Produces a deterministic payment ID "PAY-001" and marks the task as COMPLETED.
 */
public class ChargePaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "spi_charge_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().getOrDefault("orderId", "unknown");
        Object amount = task.getInputData().getOrDefault("amount", 0);

        System.out.println("  [spi_charge_payment] Charging payment for order: " + orderId + ", amount: " + amount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("paymentId", "PAY-001");
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("amount", amount);
        result.getOutputData().put("paymentStatus", "charged");

        return result;
    }
}
