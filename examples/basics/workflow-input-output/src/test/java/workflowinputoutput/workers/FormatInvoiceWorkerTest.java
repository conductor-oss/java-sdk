package workflowinputoutput.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatInvoiceWorkerTest {

    private final FormatInvoiceWorker worker = new FormatInvoiceWorker();

    @Test
    void taskDefName() {
        assertEquals("format_invoice", worker.getTaskDefName());
    }

    @Test
    void formatsCompleteInvoice() {
        Task task = taskWith(Map.ofEntries(
                Map.entry("productName", "27\" Monitor"),
                Map.entry("unitPrice", 349.99),
                Map.entry("quantity", 2),
                Map.entry("subtotal", 699.98),
                Map.entry("couponCode", "SAVE20"),
                Map.entry("discountRate", 0.20),
                Map.entry("discountAmount", 139.996),
                Map.entry("discountedTotal", 559.984),
                Map.entry("taxRate", 0.0825),
                Map.entry("taxAmount", 46.19868),
                Map.entry("finalTotal", 606.18268)
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String invoice = (String) result.getOutputData().get("invoice");
        assertNotNull(invoice);
        assertTrue(invoice.contains("27\" Monitor"));
        assertTrue(invoice.contains("SAVE20"));
        assertTrue(invoice.contains("INVOICE"));
        assertTrue(invoice.contains("TOTAL"));
    }

    @Test
    void invoiceContainsProductDetails() {
        Task task = taskWith(Map.ofEntries(
                Map.entry("productName", "USB-C Hub"),
                Map.entry("unitPrice", 49.99),
                Map.entry("quantity", 1),
                Map.entry("subtotal", 49.99),
                Map.entry("couponCode", ""),
                Map.entry("discountRate", 0.0),
                Map.entry("discountAmount", 0.0),
                Map.entry("discountedTotal", 49.99),
                Map.entry("taxRate", 0.0825),
                Map.entry("taxAmount", 4.124175),
                Map.entry("finalTotal", 54.114175)
        ));
        TaskResult result = worker.execute(task);

        String invoice = (String) result.getOutputData().get("invoice");
        assertTrue(invoice.contains("USB-C Hub"));
        assertTrue(invoice.contains("$49.99"));
        assertTrue(invoice.contains("(none)"));
    }

    @Test
    void invoiceShowsCouponWhenPresent() {
        Task task = taskWith(Map.ofEntries(
                Map.entry("productName", "Test"),
                Map.entry("unitPrice", 100.0),
                Map.entry("quantity", 1),
                Map.entry("subtotal", 100.0),
                Map.entry("couponCode", "HALF"),
                Map.entry("discountRate", 0.50),
                Map.entry("discountAmount", 50.0),
                Map.entry("discountedTotal", 50.0),
                Map.entry("taxRate", 0.0825),
                Map.entry("taxAmount", 4.125),
                Map.entry("finalTotal", 54.125)
        ));
        TaskResult result = worker.execute(task);

        String invoice = (String) result.getOutputData().get("invoice");
        assertTrue(invoice.contains("HALF"));
        assertTrue(invoice.contains("50%"));
    }

    @Test
    void handlesEmptyInputGracefully() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("invoice"));
    }

    @Test
    void outputContainsInvoiceField() {
        Task task = taskWith(Map.ofEntries(
                Map.entry("productName", "Test"),
                Map.entry("unitPrice", 10.0),
                Map.entry("quantity", 1),
                Map.entry("subtotal", 10.0),
                Map.entry("couponCode", ""),
                Map.entry("discountRate", 0.0),
                Map.entry("discountAmount", 0.0),
                Map.entry("discountedTotal", 10.0),
                Map.entry("taxRate", 0.0825),
                Map.entry("taxAmount", 0.825),
                Map.entry("finalTotal", 10.825)
        ));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("invoice"));
        assertTrue(result.getOutputData().get("invoice").toString().length() > 0);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
