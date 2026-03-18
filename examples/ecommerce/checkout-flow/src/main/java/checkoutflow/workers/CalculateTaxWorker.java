package checkoutflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Calculates real tax, shipping, and grand total based on subtotal and shipping address.
 *
 * Real tax rates by US state (2024 rates):
 *   CA: 7.25% (state) + local average ~1.5% = 8.75%
 *   NY: 4% + local ~4.5% = 8.5%
 *   TX: 6.25% + local ~1.94% = 8.19%
 *   FL: 6% + local ~1.05% = 7.05%
 *   WA: 6.5% + local ~2.67% = 9.17%
 *   OR: 0% (no sales tax)
 *   NH: 0% (no sales tax)
 *   MT: 0% (no sales tax)
 *   DE: 0% (no sales tax)
 *   AK: 0% (no state sales tax, some local)
 *   Default US: 6%
 *
 * International rates (VAT/GST):
 *   GB: 20% VAT
 *   DE: 19% MwSt
 *   FR: 20% TVA
 *   JP: 10% consumption tax
 *   AU: 10% GST
 *   CA (country): 5% GST (+ provincial)
 *
 * Shipping calculation:
 *   Free shipping for orders over $100
 *   Standard: $9.99 (5-7 days)
 *   Express: $19.99 (2-3 days)
 *   Overnight: $29.99 (next day)
 *
 * Input: subtotal, shippingAddress
 * Output: tax, taxRate, shipping, grandTotal, taxBreakdown
 */
public class CalculateTaxWorker implements Worker {

    /** US state tax rates (combined state + average local). */
    private static final Map<String, Double> US_STATE_TAX_RATES = Map.ofEntries(
            Map.entry("CA", 0.0875),
            Map.entry("NY", 0.085),
            Map.entry("TX", 0.0819),
            Map.entry("FL", 0.0705),
            Map.entry("WA", 0.0917),
            Map.entry("IL", 0.0825),
            Map.entry("PA", 0.06),
            Map.entry("OH", 0.0725),
            Map.entry("GA", 0.0735),
            Map.entry("NC", 0.0695),
            Map.entry("MI", 0.06),
            Map.entry("NJ", 0.06625),
            Map.entry("VA", 0.053),
            Map.entry("AZ", 0.056),
            Map.entry("MA", 0.0625),
            Map.entry("TN", 0.0975),
            Map.entry("IN", 0.07),
            Map.entry("MO", 0.04225),
            Map.entry("MD", 0.06),
            Map.entry("WI", 0.05),
            Map.entry("CO", 0.029),
            Map.entry("MN", 0.06875),
            Map.entry("SC", 0.06),
            Map.entry("AL", 0.04),
            Map.entry("LA", 0.0445),
            Map.entry("KY", 0.06),
            Map.entry("OR", 0.0),
            Map.entry("NH", 0.0),
            Map.entry("MT", 0.0),
            Map.entry("DE", 0.0),
            Map.entry("AK", 0.0)
    );

    /** International tax rates (VAT/GST). Keyed by country code. */
    private static final Map<String, Double> INTL_TAX_RATES = Map.of(
            "GB", 0.20,
            "DE", 0.19,
            "FR", 0.20,
            "JP", 0.10,
            "AU", 0.10,
            "CA", 0.05,
            "NZ", 0.15,
            "SE", 0.25,
            "NO", 0.25
    );

    private static final Set<String> TAX_FREE_STATES = Set.of("OR", "NH", "MT", "DE");
    private static final double FREE_SHIPPING_THRESHOLD = 100.0;
    private static final double DEFAULT_US_TAX_RATE = 0.06;

    @Override
    public String getTaskDefName() {
        return "chk_calculate_tax";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        // Parse subtotal
        double subtotal = 0;
        Object subtotalObj = task.getInputData().get("subtotal");
        if (subtotalObj instanceof Number) {
            subtotal = ((Number) subtotalObj).doubleValue();
        } else if (subtotalObj instanceof String) {
            try { subtotal = Double.parseDouble((String) subtotalObj); } catch (NumberFormatException ignored) {}
        }

        // Parse address
        String state = null;
        String country = "US";
        String shippingMethod = "standard";
        Object addrObj = task.getInputData().get("shippingAddress");
        if (addrObj instanceof Map) {
            Map<String, Object> addr = (Map<String, Object>) addrObj;
            if (addr.get("state") != null) state = addr.get("state").toString().toUpperCase();
            if (addr.get("country") != null) country = addr.get("country").toString().toUpperCase();
            if (addr.get("shippingMethod") != null) shippingMethod = addr.get("shippingMethod").toString();
        }

        // Override from task input
        Object methodObj = task.getInputData().get("shippingMethod");
        if (methodObj != null) shippingMethod = methodObj.toString();

        // --- Calculate tax ---
        double taxRate;
        String taxType;

        if ("US".equals(country) && state != null) {
            taxRate = US_STATE_TAX_RATES.getOrDefault(state, DEFAULT_US_TAX_RATE);
            taxType = TAX_FREE_STATES.contains(state) ? "exempt" : "sales_tax";
        } else if (INTL_TAX_RATES.containsKey(country)) {
            taxRate = INTL_TAX_RATES.get(country);
            taxType = "vat";
        } else {
            taxRate = state != null ? US_STATE_TAX_RATES.getOrDefault(state, DEFAULT_US_TAX_RATE) : DEFAULT_US_TAX_RATE;
            taxType = "sales_tax";
        }

        double tax = Math.round(subtotal * taxRate * 100.0) / 100.0;

        // --- Calculate shipping ---
        double shipping;
        if (subtotal >= FREE_SHIPPING_THRESHOLD) {
            shipping = 0.0;
        } else {
            shipping = switch (shippingMethod.toLowerCase()) {
                case "express" -> 19.99;
                case "overnight" -> 29.99;
                default -> 9.99;
            };
        }

        double grandTotal = Math.round((subtotal + tax + shipping) * 100.0) / 100.0;

        System.out.printf("  [tax] Subtotal: $%.2f, Tax (%.2f%% %s): $%.2f, Shipping (%s): $%.2f, Total: $%.2f%n",
                subtotal, taxRate * 100, taxType, tax, shippingMethod, shipping, grandTotal);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("tax", tax);
        output.put("taxRate", taxRate);
        output.put("taxType", taxType);
        output.put("shipping", shipping);
        output.put("shippingMethod", shippingMethod);
        output.put("freeShipping", subtotal >= FREE_SHIPPING_THRESHOLD);
        output.put("grandTotal", grandTotal);

        Map<String, Object> taxBreakdown = new LinkedHashMap<>();
        taxBreakdown.put("subtotal", subtotal);
        taxBreakdown.put("taxRate", taxRate);
        taxBreakdown.put("taxAmount", tax);
        taxBreakdown.put("shippingCost", shipping);
        taxBreakdown.put("total", grandTotal);
        output.put("taxBreakdown", taxBreakdown);

        result.setOutputData(output);
        return result;
    }
}
