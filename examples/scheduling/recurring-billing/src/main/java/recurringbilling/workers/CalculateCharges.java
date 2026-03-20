package recurringbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Calculates the total charge for a billing cycle, including prorating for
 * mid-cycle changes, loyalty discounts, and tax. All calculations use integer
 * cents to avoid floating-point rounding issues.
 */
public class CalculateCharges implements Worker {

    // Tax rate in basis points (875 = 8.75%)
    static final int TAX_RATE_BPS = 875;

    @Override
    public String getTaskDefName() {
        return "billing_calculate_charges";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[billing_calculate_charges] Calculating charges...");

        TaskResult result = new TaskResult(task);
        Map<String, Object> subscription = (Map<String, Object>) task.getInputData().get("subscription");

        if (subscription == null) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("subscription data is required");
            return result;
        }

        int monthlyPriceCents = ((Number) subscription.get("monthlyPriceCents")).intValue();
        String billingInterval = (String) subscription.get("billingInterval");
        String cycleStartStr = (String) subscription.get("billingCycleStart");
        String cycleEndStr = (String) subscription.get("billingCycleEnd");
        boolean hasLoyaltyDiscount = Boolean.TRUE.equals(subscription.get("hasLoyaltyDiscount"));
        int loyaltyDiscountPercent = ((Number) subscription.getOrDefault("loyaltyDiscountPercent", 0)).intValue();

        // Check for mid-cycle start (prorating)
        String subscriptionStartStr = (String) task.getInputData().get("subscriptionStart");
        boolean prorated = false;
        int basePriceCents;

        if ("annual".equals(billingInterval)) {
            basePriceCents = monthlyPriceCents * 12;
        } else {
            basePriceCents = monthlyPriceCents;
        }

        // Prorate if subscription started mid-cycle
        int proratedCents = basePriceCents;
        if (subscriptionStartStr != null) {
            LocalDate cycleStart = LocalDate.parse(cycleStartStr);
            LocalDate cycleEnd = LocalDate.parse(cycleEndStr);
            LocalDate subStart = LocalDate.parse(subscriptionStartStr);

            if (subStart.isAfter(cycleStart) && !subStart.isAfter(cycleEnd)) {
                long totalDays = ChronoUnit.DAYS.between(cycleStart, cycleEnd) + 1;
                long activeDays = ChronoUnit.DAYS.between(subStart, cycleEnd) + 1;
                proratedCents = (int) ((long) basePriceCents * activeDays / totalDays);
                prorated = true;
            }
        }

        // Apply loyalty discount
        int discountCents = 0;
        if (hasLoyaltyDiscount && loyaltyDiscountPercent > 0) {
            discountCents = proratedCents * loyaltyDiscountPercent / 100;
        }

        int subtotalCents = proratedCents - discountCents;

        // Calculate tax (using basis points to stay in integer math)
        int taxCents = subtotalCents * TAX_RATE_BPS / 10000;

        int totalCents = subtotalCents + taxCents;

        // Build the charge breakdown
        Map<String, Object> charges = new LinkedHashMap<>();
        charges.put("basePriceCents", basePriceCents);
        charges.put("prorated", prorated);
        charges.put("proratedCents", proratedCents);
        charges.put("discountApplied", hasLoyaltyDiscount);
        charges.put("discountPercent", loyaltyDiscountPercent);
        charges.put("discountCents", discountCents);
        charges.put("subtotalCents", subtotalCents);
        charges.put("taxRateBps", TAX_RATE_BPS);
        charges.put("taxCents", taxCents);
        charges.put("totalCents", totalCents);
        charges.put("currency", "USD");
        charges.put("billingInterval", billingInterval);

        System.out.println("  Base: $" + formatCents(basePriceCents)
                + (prorated ? " (prorated to $" + formatCents(proratedCents) + ")" : "")
                + (discountCents > 0 ? " - $" + formatCents(discountCents) + " discount" : "")
                + " + $" + formatCents(taxCents) + " tax"
                + " = $" + formatCents(totalCents));

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("charges", charges);
        return result;
    }

    private static String formatCents(int cents) {
        return String.format("%.2f", cents / 100.0);
    }
}
