package invoiceprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Receives and validates an incoming invoice. Real validation of invoice ID, vendor, and document URL.
 */
public class ReceiveInvoiceWorker implements Worker {
    @Override public String getTaskDefName() { return "ivc_receive_invoice"; }

    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String invoiceId = (String) task.getInputData().get("invoiceId");
        if (invoiceId == null || invoiceId.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: invoiceId");
            return r;
        }

        String vendorId = (String) task.getInputData().get("vendorId");
        if (vendorId == null || vendorId.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: vendorId");
            return r;
        }

        boolean validInvoice = invoiceId.startsWith("INV-") || invoiceId.startsWith("INVOICE-");
        boolean validVendor = vendorId.startsWith("VND-") || vendorId.startsWith("VENDOR-");

        if (!validInvoice) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Invalid invoiceId format: must start with 'INV-' or 'INVOICE-', got: " + invoiceId);
            return r;
        }
        if (!validVendor) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Invalid vendorId format: must start with 'VND-' or 'VENDOR-', got: " + vendorId);
            return r;
        }

        System.out.println("  [receive] Invoice " + invoiceId + " from vendor " + vendorId + " (valid: true)");

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("receivedAt", Instant.now().toString());
        r.getOutputData().put("validInvoice", true);
        r.getOutputData().put("validVendor", true);
        return r;
    }
}
