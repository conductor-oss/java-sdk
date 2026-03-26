package recurringbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

/**
 * Sends a billing notification to the customer. The notification type depends
 * on payment outcome — a receipt for successful payments, or a payment failure
 * notice with instructions to update their payment method.
 *
 * In production, this would integrate with an email service (SendGrid, SES, etc.)
 * and potentially SMS/push notifications. Here it builds the notification payload
 * deterministically.
 */
public class SendBillingNotification implements Worker {

    @Override
    public String getTaskDefName() {
        return "billing_send_notification";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[billing_send_notification] Sending billing notification...");

        TaskResult result = new TaskResult(task);

        Map<String, Object> invoice = (Map<String, Object>) task.getInputData().get("invoice");
        Map<String, Object> subscription = (Map<String, Object>) task.getInputData().get("subscription");

        if (invoice == null || subscription == null) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("invoice and subscription data are required");
            return result;
        }

        String customerEmail = (String) subscription.get("customerEmail");
        String customerName = (String) subscription.get("customerName");
        String invoiceNumber = (String) invoice.get("invoiceNumber");
        String invoiceStatus = (String) invoice.get("status");
        int totalCents = ((Number) invoice.get("totalCents")).intValue();

        // Deterministic notification ID
        String notificationId = "notif_" + Math.abs(
                (invoiceNumber + "_" + invoiceStatus).hashCode());

        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("notificationId", notificationId);
        notification.put("recipientEmail", customerEmail);
        notification.put("recipientName", customerName);
        notification.put("invoiceNumber", invoiceNumber);
        notification.put("sentAt", Instant.now().toString());

        if ("paid".equals(invoiceStatus)) {
            // Success notification — payment receipt
            notification.put("type", "payment_receipt");
            notification.put("subject", "Payment receipt for " + invoiceNumber);
            notification.put("body", buildReceiptBody(customerName, invoiceNumber, totalCents,
                    (String) invoice.get("transactionId"),
                    (Map<String, Object>) invoice.get("billingPeriod")));
            notification.put("status", "sent");

            System.out.println("  Receipt sent to " + customerEmail
                    + " for $" + formatCents(totalCents));
        } else {
            // Failure notification — payment failed
            notification.put("type", "payment_failed");
            notification.put("subject", "Action required: Payment failed for " + invoiceNumber);
            notification.put("body", buildFailureBody(customerName, invoiceNumber, totalCents,
                    (String) invoice.get("paymentFailureReason")));
            notification.put("status", "sent");
            notification.put("actionRequired", true);
            notification.put("actionUrl", "https://billing.example.com/update-payment/"
                    + subscription.get("customerId"));

            System.out.println("  Failure notice sent to " + customerEmail
                    + " — payment of $" + formatCents(totalCents) + " failed");
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notification", notification);
        return result;
    }

    static String buildReceiptBody(String customerName, String invoiceNumber,
                                   int totalCents, String transactionId,
                                   Map<String, Object> billingPeriod) {
        return "Dear " + customerName + ",\n\n"
                + "Your payment of $" + formatCents(totalCents) + " has been processed successfully.\n\n"
                + "Invoice: " + invoiceNumber + "\n"
                + "Transaction: " + transactionId + "\n"
                + "Billing period: " + billingPeriod.get("start") + " to " + billingPeriod.get("end") + "\n\n"
                + "Thank you for your continued subscription.\n\n"
                + "— Billing Team";
    }

    static String buildFailureBody(String customerName, String invoiceNumber,
                                   int totalCents, String failureReason) {
        return "Dear " + customerName + ",\n\n"
                + "We were unable to process your payment of $" + formatCents(totalCents)
                + " for invoice " + invoiceNumber + ".\n\n"
                + "Reason: " + failureReason + "\n\n"
                + "Please update your payment method to avoid service interruption.\n\n"
                + "— Billing Team";
    }

    private static String formatCents(int cents) {
        return String.format("%.2f", cents / 100.0);
    }
}
