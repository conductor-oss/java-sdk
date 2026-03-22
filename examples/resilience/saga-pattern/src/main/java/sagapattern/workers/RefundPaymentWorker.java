package sagapattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Compensation worker: refunds a previously charged payment by removing
 * the transaction entry from the BookingStore.
 *
 * Input:
 *   - tripId (String, required): trip identifier
 *
 * Output:
 *   - refunded (boolean): true
 *   - removedFromStore (boolean): whether the transaction was found and removed
 */
public class RefundPaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "saga_refund_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        String tripId = getRequiredString(task, "tripId");
        if (tripId == null || tripId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: tripId");
            return result;
        }

        String transactionId = "TXN-" + tripId;

        // Remove from booking store
        String removed = BookingStore.PAYMENT_TRANSACTIONS.remove(transactionId);
        BookingStore.recordAction("REFUND_PAYMENT", transactionId);

        System.out.println("  [refund_payment] Refunding payment " + transactionId
                + " for trip " + tripId + " (compensation) -- removed=" + (removed != null));

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("refunded", true);
        result.getOutputData().put("removedFromStore", removed != null);
        return result;
    }

    private String getRequiredString(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value == null) return null;
        return value.toString();
    }
}
