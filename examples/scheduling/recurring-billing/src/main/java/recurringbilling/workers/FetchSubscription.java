package recurringbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Fetches subscription details for a customer. In production, this would query
 * your subscription database. Here it produces deterministic output based on
 * the customerId and planId inputs.
 */
public class FetchSubscription implements Worker {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // deterministic plan catalog — maps planId to plan details
    private static final Map<String, Map<String, Object>> PLAN_CATALOG = Map.of(
            "plan_basic", Map.of(
                    "planName", "Basic",
                    "monthlyPriceCents", 999,
                    "billingInterval", "monthly",
                    "features", List.of("5 users", "10GB storage", "Email support")
            ),
            "plan_pro", Map.of(
                    "planName", "Professional",
                    "monthlyPriceCents", 2999,
                    "billingInterval", "monthly",
                    "features", List.of("25 users", "100GB storage", "Priority support", "API access")
            ),
            "plan_enterprise", Map.of(
                    "planName", "Enterprise",
                    "monthlyPriceCents", 9999,
                    "billingInterval", "monthly",
                    "features", List.of("Unlimited users", "1TB storage", "24/7 support", "API access", "SSO", "Audit logs")
            ),
            "plan_annual_pro", Map.of(
                    "planName", "Professional (Annual)",
                    "monthlyPriceCents", 2499,
                    "billingInterval", "annual",
                    "features", List.of("25 users", "100GB storage", "Priority support", "API access")
            )
    );

    @Override
    public String getTaskDefName() {
        return "billing_fetch_subscription";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("[billing_fetch_subscription] Fetching subscription details...");

        TaskResult result = new TaskResult(task);
        String customerId = (String) task.getInputData().get("customerId");
        String planId = (String) task.getInputData().get("planId");

        if (customerId == null || customerId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("customerId is required");
            return result;
        }

        if (planId == null || planId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("planId is required");
            return result;
        }

        Map<String, Object> plan = PLAN_CATALOG.get(planId);
        if (plan == null) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Unknown plan: " + planId);
            return result;
        }

        // Build subscription record deterministically from inputs
        String billingCycleStart = (String) task.getInputData().getOrDefault(
                "billingCycleStart", LocalDate.now().withDayOfMonth(1).format(DATE_FMT));
        String billingCycleEnd = (String) task.getInputData().getOrDefault(
                "billingCycleEnd", LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1).format(DATE_FMT));

        // Deterministic discount: customers ending in even digit get 10% loyalty discount
        String lastChar = customerId.substring(customerId.length() - 1);
        boolean hasLoyaltyDiscount = Character.isDigit(lastChar.charAt(0))
                && (lastChar.charAt(0) - '0') % 2 == 0;

        // Deterministic payment method based on customerId hash
        int hash = Math.abs(customerId.hashCode());
        String paymentMethodType = hash % 3 == 0 ? "credit_card" : (hash % 3 == 1 ? "bank_account" : "credit_card");
        String paymentMethodLast4 = String.format("%04d", hash % 10000);

        Map<String, Object> subscription = new LinkedHashMap<>();
        subscription.put("subscriptionId", "sub_" + customerId + "_" + planId);
        subscription.put("customerId", customerId);
        subscription.put("customerEmail", customerId + "@example.com");
        subscription.put("customerName", "Customer " + customerId.toUpperCase());
        subscription.put("planId", planId);
        subscription.put("planName", plan.get("planName"));
        subscription.put("monthlyPriceCents", plan.get("monthlyPriceCents"));
        subscription.put("billingInterval", plan.get("billingInterval"));
        subscription.put("billingCycleStart", billingCycleStart);
        subscription.put("billingCycleEnd", billingCycleEnd);
        subscription.put("status", "active");
        subscription.put("hasLoyaltyDiscount", hasLoyaltyDiscount);
        subscription.put("loyaltyDiscountPercent", hasLoyaltyDiscount ? 10 : 0);
        subscription.put("paymentMethod", Map.of(
                "type", paymentMethodType,
                "last4", paymentMethodLast4,
                "expiryMonth", 12,
                "expiryYear", 2027
        ));
        subscription.put("features", plan.get("features"));

        System.out.println("  Customer: " + subscription.get("customerName")
                + " | Plan: " + subscription.get("planName")
                + " | Price: $" + String.format("%.2f", ((int) plan.get("monthlyPriceCents")) / 100.0)
                + "/mo");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("subscription", subscription);
        return result;
    }

    /**
     * Returns the plan catalog for use in tests and other workers.
     */
    public static Map<String, Map<String, Object>> getPlanCatalog() {
        return PLAN_CATALOG;
    }
}
