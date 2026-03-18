package sagapattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Charges payment for the trip. If shouldFail is true, returns a failed status
 * to trigger saga compensation. On success, creates a real transaction entry
 * in the BookingStore.
 *
 * Output:
 * - status (String): "failed" if shouldFail is true, "success" otherwise
 * - transactionId (String): "TXN-" + tripId (only when successful)
 */
public class ChargePaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "saga_charge_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        String tripId = (String) task.getInputData().get("tripId");
        Object shouldFailObj = task.getInputData().get("shouldFail");
        boolean shouldFail = Boolean.TRUE.equals(shouldFailObj)
                || "true".equals(String.valueOf(shouldFailObj));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (shouldFail) {
            System.out.println("  [charge_payment] Payment FAILED for trip " + tripId);
            result.getOutputData().put("status", "failed");
        } else {
            String transactionId = "TXN-" + tripId;
            BookingStore.PAYMENT_TRANSACTIONS.put(transactionId, Instant.now().toString());

            System.out.println("  [charge_payment] Payment succeeded for trip " + tripId
                    + " -- transactionId=" + transactionId);
            result.getOutputData().put("status", "success");
            result.getOutputData().put("transactionId", transactionId);
        }
        return result;
    }
}
