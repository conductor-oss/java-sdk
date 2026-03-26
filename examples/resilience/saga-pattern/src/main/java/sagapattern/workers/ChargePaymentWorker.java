package sagapattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Charges payment for the trip. If shouldFail is true, returns a
 * FAILED_WITH_TERMINAL_ERROR to trigger saga compensation.
 *
 * Input:
 *   - tripId (String, required): trip identifier
 *   - shouldFail (boolean, optional): if true, simulates payment failure
 *   - amount (Number, optional): payment amount (must be positive if provided)
 *
 * Output on success:
 *   - status (String): "success"
 *   - transactionId (String): "TXN-" + tripId
 *
 * Output on failure:
 *   - status (String): "failed"
 */
public class ChargePaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "saga_charge_payment";
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

        // Validate amount if provided
        Object amountObj = task.getInputData().get("amount");
        if (amountObj != null) {
            double amount;
            if (amountObj instanceof Number) {
                amount = ((Number) amountObj).doubleValue();
            } else {
                try {
                    amount = Double.parseDouble(amountObj.toString());
                } catch (NumberFormatException e) {
                    result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
                    result.setReasonForIncompletion("Invalid amount: not a number: " + amountObj);
                    return result;
                }
            }
            if (amount <= 0) {
                result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
                result.setReasonForIncompletion("Invalid amount: must be positive, got " + amount);
                return result;
            }
        }

        Object shouldFailObj = task.getInputData().get("shouldFail");
        boolean shouldFail = Boolean.TRUE.equals(shouldFailObj)
                || "true".equals(String.valueOf(shouldFailObj));

        if (shouldFail) {
            System.out.println("  [charge_payment] Payment FAILED for trip " + tripId);
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Payment processing failed for trip " + tripId);
            result.getOutputData().put("status", "failed");
            return result;
        }

        String transactionId = "TXN-" + tripId;
        BookingStore.PAYMENT_TRANSACTIONS.put(transactionId, Instant.now().toString());
        BookingStore.recordAction("CHARGE_PAYMENT", transactionId);

        System.out.println("  [charge_payment] Payment succeeded for trip " + tripId
                + " -- transactionId=" + transactionId);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "success");
        result.getOutputData().put("transactionId", transactionId);
        return result;
    }

    private String getRequiredString(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value == null) return null;
        return value.toString();
    }
}
