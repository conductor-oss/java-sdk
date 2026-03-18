package workflowinputoutput.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Formats a text invoice from all prior task outputs.
 *
 * Input:  productName, unitPrice, quantity, subtotal,
 *         couponCode, discountRate, discountAmount, discountedTotal,
 *         taxRate, taxAmount, finalTotal
 * Output: invoice (formatted string)
 */
public class FormatInvoiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "format_invoice";
    }

    @Override
    public TaskResult execute(Task task) {
        String productName = str(task.getInputData().get("productName"));
        double unitPrice = toDouble(task.getInputData().get("unitPrice"));
        int quantity = toInt(task.getInputData().get("quantity"));
        double subtotal = toDouble(task.getInputData().get("subtotal"));
        String couponCode = str(task.getInputData().get("couponCode"));
        double discountRate = toDouble(task.getInputData().get("discountRate"));
        double discountAmount = toDouble(task.getInputData().get("discountAmount"));
        double discountedTotal = toDouble(task.getInputData().get("discountedTotal"));
        double taxRate = toDouble(task.getInputData().get("taxRate"));
        double taxAmount = toDouble(task.getInputData().get("taxAmount"));
        double finalTotal = toDouble(task.getInputData().get("finalTotal"));

        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("              INVOICE\n");
        sb.append("========================================\n");
        sb.append(String.format("Product:    %s%n", productName));
        sb.append(String.format("Unit Price: $%.2f%n", unitPrice));
        sb.append(String.format("Quantity:   %d%n", quantity));
        sb.append(String.format("Subtotal:   $%.2f%n", subtotal));
        sb.append("----------------------------------------\n");
        if (!couponCode.isEmpty()) {
            sb.append(String.format("Coupon:     %s (%.0f%% off)%n", couponCode, discountRate * 100));
            sb.append(String.format("Discount:  -$%.2f%n", discountAmount));
        } else {
            sb.append("Coupon:     (none)\n");
        }
        sb.append(String.format("After Disc: $%.2f%n", discountedTotal));
        sb.append(String.format("Tax (%.2f%%): $%.2f%n", taxRate * 100, taxAmount));
        sb.append("========================================\n");
        sb.append(String.format("TOTAL:      $%.2f%n", finalTotal));
        sb.append("========================================");

        String invoice = sb.toString();
        System.out.println("  [format_invoice] Invoice generated.");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("invoice", invoice);
        return result;
    }

    private static String str(Object value) {
        return value != null ? value.toString() : "";
    }

    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
