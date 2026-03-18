package invoiceprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Receives and validates an incoming invoice. Real validation of invoice ID and vendor.
 */
public class ReceiveInvoiceWorker implements Worker {
    @Override public String getTaskDefName() { return "ivc_receive_invoice"; }

    @Override public TaskResult execute(Task task) {
        String invoiceId = (String) task.getInputData().get("invoiceId");
        String vendorId = (String) task.getInputData().get("vendorId");
        if (invoiceId == null) invoiceId = "UNKNOWN";
        if (vendorId == null) vendorId = "UNKNOWN";

        boolean validInvoice = invoiceId.startsWith("INV-") || invoiceId.startsWith("INVOICE-");
        boolean validVendor = vendorId.startsWith("VND-") || vendorId.startsWith("VENDOR-") || !vendorId.equals("UNKNOWN");

        System.out.println("  [receive] Invoice " + invoiceId + " from vendor " + vendorId
                + " (valid: " + (validInvoice && validVendor) + ")");

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("receivedAt", Instant.now().toString());
        r.getOutputData().put("validInvoice", validInvoice);
        r.getOutputData().put("validVendor", validVendor);
        return r;
    }
}
