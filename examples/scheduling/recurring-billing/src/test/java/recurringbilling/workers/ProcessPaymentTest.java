package recurringbilling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProcessPaymentTest {

    private final ProcessPayment worker = new ProcessPayment();

    @Test
    void taskDefName() {
        assertEquals("billing_process_payment", worker.getTaskDefName());
    }

    @Test
    void successfulCreditCardPayment() {
        Task task = taskWith("cust_100", "sub_cust_100_plan_pro",
                paymentMethod("credit_card", "4242"), charges(2999));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var payment = (Map<String, Object>) result.getOutputData().get("payment");

        assertEquals("succeeded", payment.get("status"));
        assertEquals(2999, payment.get("amountCents"));
        assertEquals("USD", payment.get("currency"));
        assertEquals("credit_card", payment.get("paymentMethodType"));
        assertEquals("4242", payment.get("paymentMethodLast4"));
        assertNotNull(payment.get("transactionId"));
        assertNull(payment.get("declineCode"));
    }

    @Test
    void successfulBankAccountPayment() {
        Task task = taskWith("cust_200", "sub_cust_200_plan_basic",
                paymentMethod("bank_account", "6789"), charges(999));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var payment = (Map<String, Object>) result.getOutputData().get("payment");

        assertEquals("succeeded", payment.get("status"));
        assertEquals("bank_account", payment.get("paymentMethodType"));
    }

    @Test
    void declinedCardLast4Is0000() {
        Task task = taskWith("cust_100", "sub_cust_100_plan_pro",
                paymentMethod("credit_card", "0000"), charges(2999));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var payment = (Map<String, Object>) result.getOutputData().get("payment");

        assertEquals("declined", payment.get("status"));
        assertEquals("card_declined", payment.get("declineCode"));
        assertTrue(((String) payment.get("declineMessage")).contains("0000"));
        assertTrue((Boolean) payment.get("retryable"));
    }

    @Test
    void declinedWhenAmountExceedsLimit() {
        // $10,001 exceeds the $10,000 limit
        Task task = taskWith("cust_100", "sub_cust_100_plan_ent",
                paymentMethod("credit_card", "4242"), charges(1_000_100));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var payment = (Map<String, Object>) result.getOutputData().get("payment");

        assertEquals("declined", payment.get("status"));
        assertEquals("amount_too_large", payment.get("declineCode"));
        assertFalse((Boolean) payment.get("retryable"));
    }

    @Test
    void paymentAtExactLimitSucceeds() {
        Task task = taskWith("cust_100", "sub_cust_100_plan_ent",
                paymentMethod("credit_card", "4242"), charges(1_000_000));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var payment = (Map<String, Object>) result.getOutputData().get("payment");

        assertEquals("succeeded", payment.get("status"));
    }

    @Test
    void transactionIdIsDeterministic() {
        Task task1 = taskWith("cust_100", "sub_1", paymentMethod("credit_card", "4242"), charges(2999));
        Task task2 = taskWith("cust_100", "sub_1", paymentMethod("credit_card", "4242"), charges(2999));

        @SuppressWarnings("unchecked")
        var payment1 = (Map<String, Object>) worker.execute(task1).getOutputData().get("payment");
        @SuppressWarnings("unchecked")
        var payment2 = (Map<String, Object>) worker.execute(task2).getOutputData().get("payment");

        assertEquals(payment1.get("transactionId"), payment2.get("transactionId"));
    }

    @Test
    void differentAmountsProduceDifferentTransactionIds() {
        Task task1 = taskWith("cust_100", "sub_1", paymentMethod("credit_card", "4242"), charges(2999));
        Task task2 = taskWith("cust_100", "sub_1", paymentMethod("credit_card", "4242"), charges(5000));

        @SuppressWarnings("unchecked")
        var payment1 = (Map<String, Object>) worker.execute(task1).getOutputData().get("payment");
        @SuppressWarnings("unchecked")
        var payment2 = (Map<String, Object>) worker.execute(task2).getOutputData().get("payment");

        assertNotEquals(payment1.get("transactionId"), payment2.get("transactionId"));
    }

    @Test
    void failsWhenSubscriptionMissing() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("charges", charges(2999))));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @Test
    void failsWhenChargesMissing() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> sub = new HashMap<>();
        sub.put("customerId", "cust_100");
        sub.put("subscriptionId", "sub_1");
        sub.put("paymentMethod", paymentMethod("credit_card", "4242"));
        task.setInputData(new HashMap<>(Map.of("subscription", sub)));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @Test
    void failsWhenNoPaymentMethod() {
        Map<String, Object> sub = new HashMap<>();
        sub.put("customerId", "cust_100");
        sub.put("subscriptionId", "sub_1");
        // No paymentMethod key

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("subscription", sub);
        input.put("charges", charges(2999));
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("payment method"));
    }

    private Task taskWith(String customerId, String subscriptionId,
                          Map<String, Object> paymentMethod, Map<String, Object> charges) {
        Map<String, Object> sub = new LinkedHashMap<>();
        sub.put("customerId", customerId);
        sub.put("subscriptionId", subscriptionId);
        sub.put("paymentMethod", paymentMethod);

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("subscription", sub);
        input.put("charges", charges);
        task.setInputData(input);
        return task;
    }

    private Map<String, Object> paymentMethod(String type, String last4) {
        return Map.of("type", type, "last4", last4, "expiryMonth", 12, "expiryYear", 2027);
    }

    private Map<String, Object> charges(int totalCents) {
        Map<String, Object> charges = new LinkedHashMap<>();
        charges.put("totalCents", totalCents);
        charges.put("currency", "USD");
        return charges;
    }
}
