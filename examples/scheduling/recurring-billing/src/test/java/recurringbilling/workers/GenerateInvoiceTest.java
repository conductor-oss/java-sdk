package recurringbilling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GenerateInvoiceTest {

    private final GenerateInvoice worker = new GenerateInvoice();

    @Test
    void taskDefName() {
        assertEquals("billing_generate_invoice", worker.getTaskDefName());
    }

    @Test
    void generatesInvoiceForSuccessfulPayment() {
        Task task = taskWith(
                subscription("cust_100", "sub_cust_100_plan_pro", "Professional"),
                charges(2999, 0, 2999, 262, 3261, false),
                payment("succeeded", "txn_12345")
        );

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var invoice = (Map<String, Object>) result.getOutputData().get("invoice");

        assertNotNull(invoice.get("invoiceNumber"));
        assertTrue(((String) invoice.get("invoiceNumber")).startsWith("INV-"));
        assertEquals("cust_100", invoice.get("customerId"));
        assertEquals("Customer CUST_100", invoice.get("customerName"));
        assertEquals("paid", invoice.get("status"));
        assertEquals("succeeded", invoice.get("paymentStatus"));
        assertEquals(3261, invoice.get("totalCents"));
        assertEquals("USD", invoice.get("currency"));
        assertEquals("txn_12345", invoice.get("transactionId"));
    }

    @Test
    void generatesInvoiceForDeclinedPayment() {
        Task task = taskWith(
                subscription("cust_200", "sub_cust_200_plan_basic", "Basic"),
                charges(999, 0, 999, 87, 1086, false),
                declinedPayment("txn_99999", "The card was declined")
        );

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var invoice = (Map<String, Object>) result.getOutputData().get("invoice");

        assertEquals("payment_failed", invoice.get("status"));
        assertEquals("declined", invoice.get("paymentStatus"));
        assertEquals("The card was declined", invoice.get("paymentFailureReason"));
    }

    @Test
    void invoiceContainsLineItems() {
        Task task = taskWith(
                subscription("cust_100", "sub_1", "Professional"),
                charges(2999, 0, 2999, 262, 3261, false),
                payment("succeeded", "txn_1")
        );

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var invoice = (Map<String, Object>) result.getOutputData().get("invoice");
        @SuppressWarnings("unchecked")
        var lineItems = (List<Map<String, Object>>) invoice.get("lineItems");

        // Should have subscription line + tax line (no discount)
        assertEquals(2, lineItems.size());
        assertTrue(((String) lineItems.get(0).get("description")).contains("Professional"));
        assertTrue(((String) lineItems.get(1).get("description")).contains("Tax"));
    }

    @Test
    void invoiceContainsDiscountLineItem() {
        Task task = taskWith(
                subscription("cust_100", "sub_1", "Professional"),
                charges(2999, 299, 2700, 236, 2936, true),
                payment("succeeded", "txn_1")
        );

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var invoice = (Map<String, Object>) result.getOutputData().get("invoice");
        @SuppressWarnings("unchecked")
        var lineItems = (List<Map<String, Object>>) invoice.get("lineItems");

        // Should have subscription line + discount line + tax line
        assertEquals(3, lineItems.size());
        assertTrue(((String) lineItems.get(1).get("description")).contains("Loyalty discount"));
        assertEquals(-299, lineItems.get(1).get("amountCents"));
    }

    @Test
    void invoiceContainsBillingPeriod() {
        Task task = taskWith(
                subscription("cust_100", "sub_1", "Pro"),
                charges(2999, 0, 2999, 262, 3261, false),
                payment("succeeded", "txn_1")
        );

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var invoice = (Map<String, Object>) result.getOutputData().get("invoice");
        @SuppressWarnings("unchecked")
        var period = (Map<String, Object>) invoice.get("billingPeriod");

        assertEquals("2025-03-01", period.get("start"));
        assertEquals("2025-03-31", period.get("end"));
    }

    @Test
    void invoiceNumberIsDeterministic() {
        Task task1 = taskWith(
                subscription("cust_100", "sub_1", "Pro"),
                charges(2999, 0, 2999, 262, 3261, false),
                payment("succeeded", "txn_1")
        );
        Task task2 = taskWith(
                subscription("cust_100", "sub_1", "Pro"),
                charges(2999, 0, 2999, 262, 3261, false),
                payment("succeeded", "txn_1")
        );

        @SuppressWarnings("unchecked")
        var inv1 = (Map<String, Object>) worker.execute(task1).getOutputData().get("invoice");
        @SuppressWarnings("unchecked")
        var inv2 = (Map<String, Object>) worker.execute(task2).getOutputData().get("invoice");

        assertEquals(inv1.get("invoiceNumber"), inv2.get("invoiceNumber"));
    }

    @Test
    void proratedInvoiceIncludesNote() {
        Map<String, Object> chargesMap = charges(2999, 0, 1500, 131, 1631, false);
        chargesMap.put("prorated", true);
        chargesMap.put("basePriceCents", 2999);

        Task task = taskWith(
                subscription("cust_100", "sub_1", "Professional"),
                chargesMap,
                payment("succeeded", "txn_1")
        );

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var invoice = (Map<String, Object>) result.getOutputData().get("invoice");
        @SuppressWarnings("unchecked")
        var lineItems = (List<Map<String, Object>>) invoice.get("lineItems");

        assertNotNull(lineItems.get(0).get("note"));
        assertTrue(((String) lineItems.get(0).get("note")).contains("Prorated"));
    }

    @Test
    void failsWhenInputsMissing() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    private Map<String, Object> subscription(String customerId, String subscriptionId, String planName) {
        Map<String, Object> sub = new LinkedHashMap<>();
        sub.put("customerId", customerId);
        sub.put("subscriptionId", subscriptionId);
        sub.put("customerName", "Customer " + customerId.toUpperCase());
        sub.put("customerEmail", customerId + "@example.com");
        sub.put("planName", planName);
        sub.put("billingCycleStart", "2025-03-01");
        sub.put("billingCycleEnd", "2025-03-31");
        return sub;
    }

    private Map<String, Object> charges(int proratedCents, int discountCents,
                                        int subtotalCents, int taxCents, int totalCents,
                                        boolean hasDiscount) {
        Map<String, Object> charges = new LinkedHashMap<>();
        charges.put("basePriceCents", proratedCents);
        charges.put("prorated", false);
        charges.put("proratedCents", proratedCents);
        charges.put("discountApplied", hasDiscount);
        charges.put("discountPercent", hasDiscount ? 10 : 0);
        charges.put("discountCents", discountCents);
        charges.put("subtotalCents", subtotalCents);
        charges.put("taxRateBps", 875);
        charges.put("taxCents", taxCents);
        charges.put("totalCents", totalCents);
        charges.put("currency", "USD");
        return charges;
    }

    private Map<String, Object> payment(String status, String transactionId) {
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("status", status);
        payment.put("transactionId", transactionId);
        return payment;
    }

    private Map<String, Object> declinedPayment(String transactionId, String declineMessage) {
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("status", "declined");
        payment.put("transactionId", transactionId);
        payment.put("declineMessage", declineMessage);
        return payment;
    }

    private Task taskWith(Map<String, Object> subscription,
                          Map<String, Object> charges,
                          Map<String, Object> payment) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("subscription", subscription);
        input.put("charges", charges);
        input.put("payment", payment);
        task.setInputData(input);
        return task;
    }
}
