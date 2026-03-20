package recurringbilling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FetchSubscriptionTest {

    private final FetchSubscription worker = new FetchSubscription();

    @Test
    void taskDefName() {
        assertEquals("billing_fetch_subscription", worker.getTaskDefName());
    }

    @Test
    void fetchesBasicPlanSubscription() {
        Task task = taskWith("cust_100", "plan_basic");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var sub = (Map<String, Object>) result.getOutputData().get("subscription");
        assertNotNull(sub);
        assertEquals("cust_100", sub.get("customerId"));
        assertEquals("plan_basic", sub.get("planId"));
        assertEquals("Basic", sub.get("planName"));
        assertEquals(999, sub.get("monthlyPriceCents"));
        assertEquals("monthly", sub.get("billingInterval"));
        assertEquals("active", sub.get("status"));
    }

    @Test
    void fetchesProPlanSubscription() {
        Task task = taskWith("cust_200", "plan_pro");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var sub = (Map<String, Object>) result.getOutputData().get("subscription");
        assertEquals("Professional", sub.get("planName"));
        assertEquals(2999, sub.get("monthlyPriceCents"));
    }

    @Test
    void fetchesEnterprisePlanSubscription() {
        Task task = taskWith("cust_300", "plan_enterprise");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var sub = (Map<String, Object>) result.getOutputData().get("subscription");
        assertEquals("Enterprise", sub.get("planName"));
        assertEquals(9999, sub.get("monthlyPriceCents"));
    }

    @Test
    void fetchesAnnualProPlanSubscription() {
        Task task = taskWith("cust_400", "plan_annual_pro");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var sub = (Map<String, Object>) result.getOutputData().get("subscription");
        assertEquals("Professional (Annual)", sub.get("planName"));
        assertEquals(2499, sub.get("monthlyPriceCents"));
        assertEquals("annual", sub.get("billingInterval"));
    }

    @Test
    void evenCustomerIdGetsLoyaltyDiscount() {
        // "cust_102" ends in '2' which is even
        Task task = taskWith("cust_102", "plan_pro");
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var sub = (Map<String, Object>) result.getOutputData().get("subscription");
        assertTrue((Boolean) sub.get("hasLoyaltyDiscount"));
        assertEquals(10, sub.get("loyaltyDiscountPercent"));
    }

    @Test
    void oddCustomerIdGetsNoLoyaltyDiscount() {
        // "cust_103" ends in '3' which is odd
        Task task = taskWith("cust_103", "plan_pro");
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var sub = (Map<String, Object>) result.getOutputData().get("subscription");
        assertFalse((Boolean) sub.get("hasLoyaltyDiscount"));
        assertEquals(0, sub.get("loyaltyDiscountPercent"));
    }

    @Test
    void subscriptionIdIsDeterministic() {
        Task task1 = taskWith("cust_100", "plan_pro");
        Task task2 = taskWith("cust_100", "plan_pro");

        @SuppressWarnings("unchecked")
        var sub1 = (Map<String, Object>) worker.execute(task1).getOutputData().get("subscription");
        @SuppressWarnings("unchecked")
        var sub2 = (Map<String, Object>) worker.execute(task2).getOutputData().get("subscription");

        assertEquals(sub1.get("subscriptionId"), sub2.get("subscriptionId"));
        assertEquals("sub_cust_100_plan_pro", sub1.get("subscriptionId"));
    }

    @Test
    void paymentMethodIncluded() {
        Task task = taskWith("cust_100", "plan_basic");
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var sub = (Map<String, Object>) result.getOutputData().get("subscription");
        @SuppressWarnings("unchecked")
        var pm = (Map<String, Object>) sub.get("paymentMethod");

        assertNotNull(pm);
        assertNotNull(pm.get("type"));
        assertNotNull(pm.get("last4"));
        assertTrue(((String) pm.get("last4")).matches("\\d{4}"));
    }

    @Test
    void failsWhenCustomerIdMissing() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("planId", "plan_basic")));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("customerId"));
    }

    @Test
    void failsWhenPlanIdMissing() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("customerId", "cust_100")));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("planId"));
    }

    @Test
    void failsForUnknownPlan() {
        Task task = taskWith("cust_100", "plan_nonexistent");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("Unknown plan"));
    }

    @Test
    void customerEmailDerivedFromId() {
        Task task = taskWith("cust_100", "plan_basic");
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var sub = (Map<String, Object>) result.getOutputData().get("subscription");
        assertEquals("cust_100@example.com", sub.get("customerEmail"));
    }

    private Task taskWith(String customerId, String planId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", customerId);
        input.put("planId", planId);
        task.setInputData(input);
        return task;
    }
}
