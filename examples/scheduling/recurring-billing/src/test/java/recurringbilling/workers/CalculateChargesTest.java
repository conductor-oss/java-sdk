package recurringbilling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CalculateChargesTest {

    private final CalculateCharges worker = new CalculateCharges();

    @Test
    void taskDefName() {
        assertEquals("billing_calculate_charges", worker.getTaskDefName());
    }

    @Test
    void monthlyChargeWithoutDiscount() {
        Map<String, Object> subscription = subscription(2999, "monthly", false, 0);
        Task task = taskWith(subscription, null);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var charges = (Map<String, Object>) result.getOutputData().get("charges");

        assertEquals(2999, charges.get("basePriceCents"));
        assertFalse((Boolean) charges.get("prorated"));
        assertEquals(2999, charges.get("proratedCents"));
        assertFalse((Boolean) charges.get("discountApplied"));
        assertEquals(0, charges.get("discountCents"));
        assertEquals(2999, charges.get("subtotalCents"));
        // Tax: 2999 * 875 / 10000 = 262 (integer division)
        assertEquals(262, charges.get("taxCents"));
        assertEquals(2999 + 262, charges.get("totalCents"));
        assertEquals("USD", charges.get("currency"));
    }

    @Test
    void monthlyChargeWithLoyaltyDiscount() {
        Map<String, Object> subscription = subscription(2999, "monthly", true, 10);
        Task task = taskWith(subscription, null);

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var charges = (Map<String, Object>) result.getOutputData().get("charges");

        // 10% discount: 2999 * 10 / 100 = 299
        assertEquals(299, charges.get("discountCents"));
        // Subtotal: 2999 - 299 = 2700
        assertEquals(2700, charges.get("subtotalCents"));
        // Tax: 2700 * 875 / 10000 = 236
        assertEquals(236, charges.get("taxCents"));
        assertEquals(2700 + 236, charges.get("totalCents"));
        assertTrue((Boolean) charges.get("discountApplied"));
        assertEquals(10, charges.get("discountPercent"));
    }

    @Test
    void annualBillingMultipliesMonthlyPrice() {
        Map<String, Object> subscription = subscription(2499, "annual", false, 0);
        Task task = taskWith(subscription, null);

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var charges = (Map<String, Object>) result.getOutputData().get("charges");

        // Annual = 2499 * 12 = 29988
        assertEquals(29988, charges.get("basePriceCents"));
        assertEquals(29988, charges.get("proratedCents"));
        assertEquals("annual", charges.get("billingInterval"));
    }

    @Test
    void proratingForMidCycleStart() {
        // Cycle: 2025-03-01 to 2025-03-31 (31 days)
        // Subscription start: 2025-03-16 (16 active days out of 31)
        Map<String, Object> subscription = subscription(3100, "monthly", false, 0);
        subscription.put("billingCycleStart", "2025-03-01");
        subscription.put("billingCycleEnd", "2025-03-31");
        Task task = taskWith(subscription, "2025-03-16");

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var charges = (Map<String, Object>) result.getOutputData().get("charges");

        assertTrue((Boolean) charges.get("prorated"));
        // 3100 * 16 / 31 = 1600
        assertEquals(1600, charges.get("proratedCents"));
    }

    @Test
    void noProratingWhenSubscriptionStartsBeforeCycle() {
        Map<String, Object> subscription = subscription(2999, "monthly", false, 0);
        subscription.put("billingCycleStart", "2025-03-01");
        subscription.put("billingCycleEnd", "2025-03-31");
        // Subscription started before the cycle — no prorating
        Task task = taskWith(subscription, "2025-02-15");

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var charges = (Map<String, Object>) result.getOutputData().get("charges");

        assertFalse((Boolean) charges.get("prorated"));
        assertEquals(2999, charges.get("proratedCents"));
    }

    @Test
    void noProratingWhenSubscriptionStartsOnCycleStart() {
        Map<String, Object> subscription = subscription(2999, "monthly", false, 0);
        subscription.put("billingCycleStart", "2025-03-01");
        subscription.put("billingCycleEnd", "2025-03-31");
        Task task = taskWith(subscription, "2025-03-01");

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var charges = (Map<String, Object>) result.getOutputData().get("charges");

        assertFalse((Boolean) charges.get("prorated"));
    }

    @Test
    void proratingWithDiscount() {
        // Cycle: 2025-04-01 to 2025-04-30 (30 days)
        // Sub start: 2025-04-16 (15 active days)
        Map<String, Object> subscription = subscription(3000, "monthly", true, 10);
        subscription.put("billingCycleStart", "2025-04-01");
        subscription.put("billingCycleEnd", "2025-04-30");
        Task task = taskWith(subscription, "2025-04-16");

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var charges = (Map<String, Object>) result.getOutputData().get("charges");

        assertTrue((Boolean) charges.get("prorated"));
        // 3000 * 15 / 30 = 1500
        assertEquals(1500, charges.get("proratedCents"));
        // Discount: 1500 * 10 / 100 = 150
        assertEquals(150, charges.get("discountCents"));
        // Subtotal: 1500 - 150 = 1350
        assertEquals(1350, charges.get("subtotalCents"));
    }

    @Test
    void taxRateIsCorrect() {
        // Verify the tax rate constant
        assertEquals(875, CalculateCharges.TAX_RATE_BPS);
    }

    @Test
    void failsWhenSubscriptionMissing() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("subscription"));
    }

    @Test
    void basicPlanChargeBreakdown() {
        // Basic plan at $9.99/mo, no discount
        Map<String, Object> subscription = subscription(999, "monthly", false, 0);
        Task task = taskWith(subscription, null);

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var charges = (Map<String, Object>) result.getOutputData().get("charges");

        assertEquals(999, charges.get("basePriceCents"));
        assertEquals(999, charges.get("subtotalCents"));
        // Tax: 999 * 875 / 10000 = 87
        assertEquals(87, charges.get("taxCents"));
        assertEquals(999 + 87, charges.get("totalCents"));
    }

    private Map<String, Object> subscription(int monthlyPriceCents, String billingInterval,
                                             boolean hasLoyaltyDiscount, int loyaltyDiscountPercent) {
        Map<String, Object> sub = new LinkedHashMap<>();
        sub.put("monthlyPriceCents", monthlyPriceCents);
        sub.put("billingInterval", billingInterval);
        sub.put("billingCycleStart", "2025-03-01");
        sub.put("billingCycleEnd", "2025-03-31");
        sub.put("hasLoyaltyDiscount", hasLoyaltyDiscount);
        sub.put("loyaltyDiscountPercent", loyaltyDiscountPercent);
        return sub;
    }

    private Task taskWith(Map<String, Object> subscription, String subscriptionStart) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("subscription", subscription);
        if (subscriptionStart != null) {
            input.put("subscriptionStart", subscriptionStart);
        }
        task.setInputData(input);
        return task;
    }
}
