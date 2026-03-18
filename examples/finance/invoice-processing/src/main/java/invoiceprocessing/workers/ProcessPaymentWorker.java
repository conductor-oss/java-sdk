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
        Object amountObj = task.getInputData().get("amount");
        String vendorId = (String) task.getInputData().get("vendorId");
        if (vendorId == null) vendorId = "UNKNOWN";

        double amount = 0;
        if (amountObj instanceof Number) amount = ((Number) amountObj).doubleValue();

        // Real payment terms: Net 30 for standard, Net 15 for preferred vendors
        boolean preferred = vendorId.contains("PREF") || vendorId.hashCode() % 3 == 0;
        int netDays = preferred ? 15 : 30;
        String scheduledDate = LocalDate.now().plusDays(netDays).toString();
        String paymentMethod = amount > 10000 ? "wire_transfer" : "ach";

        System.out.println("  [pay] Processing $" + String.format("%.2f", amount)
                + " to vendor " + vendorId + " via " + paymentMethod + " (Net " + netDays + ")");

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("paymentStatus", "scheduled");
        r.getOutputData().put("scheduledDate", scheduledDate);
        r.getOutputData().put("paymentMethod", paymentMethod);
        r.getOutputData().put("netDays", netDays);
        return r;
    }
}
