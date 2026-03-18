package sagapattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Compensation worker: refunds a previously charged payment by removing
 * the transaction entry from the BookingStore.
 *
 * Output:
 * - refunded (boolean): true
 * - removedFromStore (boolean): whether the transaction was found and removed
 */
public class RefundPaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "saga_refund_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        String tripId = (String) task.getInputData().get("tripId");
        String transactionId = "TXN-" + tripId;

        // Remove from booking store
        String removed = BookingStore.PAYMENT_TRANSACTIONS.remove(transactionId);

        System.out.println("  [refund_payment] Refunding payment " + transactionId
                + " for trip " + tripId + " (compensation) -- removed=" + (removed != null));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("refunded", true);
        result.getOutputData().put("removedFromStore", removed != null);
        return result;
    }
}
