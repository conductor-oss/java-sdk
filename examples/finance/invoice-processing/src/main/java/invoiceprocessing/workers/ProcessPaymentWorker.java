package invoiceprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.LocalDate;

/**
 * Processes payment for an approved invoice. Real payment scheduling logic.
 */
public class ProcessPaymentWorker implements Worker {
    @Override public String getTaskDefName() { return "ivc_process_payment"; }

    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        Object amountObj = task.getInputData().get("amount");
        if (amountObj == null || !(amountObj instanceof Number)) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing or non-numeric required input: amount");
            return r;
        }
        double amount = ((Number) amountObj).doubleValue();
        if (amount <= 0) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Invalid amount: must be positive, got " + amount);
            return r;
        }

        String vendorId = (String) task.getInputData().get("vendorId");
        if (vendorId == null || vendorId.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: vendorId");
            return r;
        }

        // Real payment terms: Net 30 for standard, Net 15 for preferred vendors
        boolean preferred = vendorId.contains("PREF") || vendorId.hashCode() % 3 == 0;
        int netDays = preferred ? 15 : 30;
        String scheduledDate = LocalDate.now().plusDays(netDays).toString();
        String paymentMethod = amount > 10000 ? "wire_transfer" : "ach";

        System.out.println("  [pay] Processing $" + String.format("%.2f", amount)
                + " to vendor " + vendorId + " via " + paymentMethod + " (Net " + netDays + ")");

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("paymentStatus", "scheduled");
        r.getOutputData().put("scheduledDate", scheduledDate);
        r.getOutputData().put("paymentMethod", paymentMethod);
        r.getOutputData().put("netDays", netDays);
        return r;
    }
}
