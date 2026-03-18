package procurementworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Processes payment for a purchase order. Real payment ID generation.
 */
public class PayWorker implements Worker {
    @Override public String getTaskDefName() { return "prw_pay"; }

    @Override public TaskResult execute(Task task) {
        String poNumber = (String) task.getInputData().get("poNumber");
        Object amountObj = task.getInputData().get("amount");
        double amount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : 0;

        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        boolean paid = amount > 0 && poNumber != null && !poNumber.equals("NONE");

        System.out.println("  [pay] " + (paid ? "Payment " + paymentId + " of $" + (int) amount : "No payment") + " for " + poNumber);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("paymentId", paymentId);
        result.getOutputData().put("paid", paid);
        result.getOutputData().put("paidAt", paid ? Instant.now().toString() : null);
        return result;
    }
}
