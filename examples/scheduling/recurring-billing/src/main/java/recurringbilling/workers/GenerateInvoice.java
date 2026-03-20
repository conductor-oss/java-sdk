package recurringbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

/**
 * Generates an invoice document from the subscription, charge breakdown, and
 * payment result. The invoice captures the full billing record — line items,
 * discounts, taxes, payment status, and a deterministic invoice number.
 *
 * In production, this would persist the invoice to a database and/or generate
 * a PDF. Here it builds a structured invoice map.
 */
public class GenerateInvoice implements Worker {

    @Override
    public String getTaskDefName() {
        return "billing_generate_invoice";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[billing_generate_invoice] Generating invoice...");

        TaskResult result = new TaskResult(task);

        Map<String, Object> subscription = (Map<String, Object>) task.getInputData().get("subscription");
        Map<String, Object> charges = (Map<String, Object>) task.getInputData().get("charges");
        Map<String, Object> payment = (Map<String, Object>) task.getInputData().get("payment");

        if (subscription == null || charges == null || payment == null) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("subscription, charges, and payment data are required");
            return result;
        }

        String customerId = (String) subscription.get("customerId");
        String subscriptionId = (String) subscription.get("subscriptionId");
        String paymentStatus = (String) payment.get("status");

        // Deterministic invoice number from subscription + cycle
        String invoiceNumber = "INV-" + Math.abs(
                (subscriptionId + "_" + subscription.get("billingCycleStart")).hashCode());

        // Build line items
        List<Map<String, Object>> lineItems = new ArrayList<>();

        // Main subscription line item
        Map<String, Object> subscriptionLine = new LinkedHashMap<>();
        subscriptionLine.put("description", subscription.get("planName") + " subscription");
        subscriptionLine.put("period", subscription.get("billingCycleStart")
                + " to " + subscription.get("billingCycleEnd"));
        subscriptionLine.put("amountCents", charges.get("proratedCents"));
        if (Boolean.TRUE.equals(charges.get("prorated"))) {
            subscriptionLine.put("note", "Prorated from base price of $"
                    + formatCents(((Number) charges.get("basePriceCents")).intValue()));
        }
        lineItems.add(subscriptionLine);

        // Discount line item (if applicable)
        int discountCents = ((Number) charges.get("discountCents")).intValue();
        if (discountCents > 0) {
            Map<String, Object> discountLine = new LinkedHashMap<>();
            discountLine.put("description", "Loyalty discount ("
                    + charges.get("discountPercent") + "%)");
            discountLine.put("amountCents", -discountCents);
            lineItems.add(discountLine);
        }

        // Tax line item
        Map<String, Object> taxLine = new LinkedHashMap<>();
        int taxRateBps = ((Number) charges.get("taxRateBps")).intValue();
        taxLine.put("description", "Tax (" + formatBpsAsPercent(taxRateBps) + "%)");
        taxLine.put("amountCents", charges.get("taxCents"));
        lineItems.add(taxLine);

        // Build the invoice
        Map<String, Object> invoice = new LinkedHashMap<>();
        invoice.put("invoiceNumber", invoiceNumber);
        invoice.put("customerId", customerId);
        invoice.put("customerName", subscription.get("customerName"));
        invoice.put("customerEmail", subscription.get("customerEmail"));
        invoice.put("subscriptionId", subscriptionId);
        invoice.put("billingPeriod", Map.of(
                "start", subscription.get("billingCycleStart"),
                "end", subscription.get("billingCycleEnd")
        ));
        invoice.put("lineItems", lineItems);
        invoice.put("subtotalCents", charges.get("subtotalCents"));
        invoice.put("taxCents", charges.get("taxCents"));
        invoice.put("totalCents", charges.get("totalCents"));
        invoice.put("currency", charges.getOrDefault("currency", "USD"));
        invoice.put("paymentStatus", paymentStatus);
        invoice.put("transactionId", payment.get("transactionId"));
        invoice.put("issuedAt", Instant.now().toString());

        if ("declined".equals(paymentStatus)) {
            invoice.put("paymentFailureReason", payment.get("declineMessage"));
            invoice.put("status", "payment_failed");
        } else {
            invoice.put("status", "paid");
        }

        System.out.println("  Invoice: " + invoiceNumber
                + " | $" + formatCents(((Number) charges.get("totalCents")).intValue())
                + " | " + invoice.get("status"));

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("invoice", invoice);
        return result;
    }

    private static String formatCents(int cents) {
        return String.format("%.2f", cents / 100.0);
    }

    private static String formatBpsAsPercent(int bps) {
        return String.format("%.2f", bps / 100.0);
    }
}
