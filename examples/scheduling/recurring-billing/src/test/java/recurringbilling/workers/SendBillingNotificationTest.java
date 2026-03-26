package recurringbilling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SendBillingNotificationTest {

    private final SendBillingNotification worker = new SendBillingNotification();

    @Test
    void taskDefName() {
        assertEquals("billing_send_notification", worker.getTaskDefName());
    }

    @Test
    void sendsReceiptForSuccessfulPayment() {
        Task task = taskWith(
                paidInvoice("INV-12345", 3261, "txn_abc"),
                subscription("cust_100", "Customer CUST_100", "cust_100@example.com")
        );

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var notification = (Map<String, Object>) result.getOutputData().get("notification");

        assertEquals("payment_receipt", notification.get("type"));
        assertEquals("cust_100@example.com", notification.get("recipientEmail"));
        assertEquals("Customer CUST_100", notification.get("recipientName"));
        assertEquals("INV-12345", notification.get("invoiceNumber"));
        assertEquals("sent", notification.get("status"));
        assertTrue(((String) notification.get("subject")).contains("Payment receipt"));
        assertTrue(((String) notification.get("body")).contains("$32.61"));
        assertTrue(((String) notification.get("body")).contains("txn_abc"));
        assertNull(notification.get("actionRequired"));
    }

    @Test
    void sendsFailureNoticeForDeclinedPayment() {
        Task task = taskWith(
                failedInvoice("INV-99999", 1086, "Card declined"),
                subscription("cust_200", "Customer CUST_200", "cust_200@example.com")
        );

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var notification = (Map<String, Object>) result.getOutputData().get("notification");

        assertEquals("payment_failed", notification.get("type"));
        assertTrue(((String) notification.get("subject")).contains("Action required"));
        assertTrue(((String) notification.get("body")).contains("Card declined"));
        assertTrue(((String) notification.get("body")).contains("$10.86"));
        assertTrue((Boolean) notification.get("actionRequired"));
        assertTrue(((String) notification.get("actionUrl")).contains("cust_200"));
    }

    @Test
    void notificationIdIsDeterministic() {
        Task task1 = taskWith(
                paidInvoice("INV-100", 3261, "txn_1"),
                subscription("cust_100", "Customer", "c@test.com")
        );
        Task task2 = taskWith(
                paidInvoice("INV-100", 3261, "txn_1"),
                subscription("cust_100", "Customer", "c@test.com")
        );

        @SuppressWarnings("unchecked")
        var n1 = (Map<String, Object>) worker.execute(task1).getOutputData().get("notification");
        @SuppressWarnings("unchecked")
        var n2 = (Map<String, Object>) worker.execute(task2).getOutputData().get("notification");

        assertEquals(n1.get("notificationId"), n2.get("notificationId"));
    }

    @Test
    void receiptBodyContainsBillingPeriod() {
        String body = SendBillingNotification.buildReceiptBody(
                "Alice", "INV-1", 2999, "txn_abc",
                Map.of("start", "2025-03-01", "end", "2025-03-31"));

        assertTrue(body.contains("Alice"));
        assertTrue(body.contains("$29.99"));
        assertTrue(body.contains("INV-1"));
        assertTrue(body.contains("txn_abc"));
        assertTrue(body.contains("2025-03-01"));
        assertTrue(body.contains("2025-03-31"));
    }

    @Test
    void failureBodyContainsReason() {
        String body = SendBillingNotification.buildFailureBody(
                "Bob", "INV-2", 999, "Insufficient funds");

        assertTrue(body.contains("Bob"));
        assertTrue(body.contains("$9.99"));
        assertTrue(body.contains("INV-2"));
        assertTrue(body.contains("Insufficient funds"));
        assertTrue(body.contains("update your payment method"));
    }

    @Test
    void failsWhenInvoiceMissing() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("subscription", subscription("cust_1", "C", "c@t.com"));
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @Test
    void failsWhenSubscriptionMissing() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("invoice", paidInvoice("INV-1", 100, "txn_1"));
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    private Map<String, Object> paidInvoice(String invoiceNumber, int totalCents, String transactionId) {
        Map<String, Object> invoice = new LinkedHashMap<>();
        invoice.put("invoiceNumber", invoiceNumber);
        invoice.put("totalCents", totalCents);
        invoice.put("status", "paid");
        invoice.put("paymentStatus", "succeeded");
        invoice.put("transactionId", transactionId);
        invoice.put("billingPeriod", Map.of("start", "2025-03-01", "end", "2025-03-31"));
        return invoice;
    }

    private Map<String, Object> failedInvoice(String invoiceNumber, int totalCents, String failureReason) {
        Map<String, Object> invoice = new LinkedHashMap<>();
        invoice.put("invoiceNumber", invoiceNumber);
        invoice.put("totalCents", totalCents);
        invoice.put("status", "payment_failed");
        invoice.put("paymentStatus", "declined");
        invoice.put("paymentFailureReason", failureReason);
        return invoice;
    }

    private Map<String, Object> subscription(String customerId, String customerName, String customerEmail) {
        Map<String, Object> sub = new LinkedHashMap<>();
        sub.put("customerId", customerId);
        sub.put("customerName", customerName);
        sub.put("customerEmail", customerEmail);
        return sub;
    }

    private Task taskWith(Map<String, Object> invoice, Map<String, Object> subscription) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("invoice", invoice);
        input.put("subscription", subscription);
        task.setInputData(input);
        return task;
    }
}
