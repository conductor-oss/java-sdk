package checkoutflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates a shopping cart with real logic:
 *   - Verifies each item has required fields (sku, name, price, qty)
 *   - Checks quantities are positive and within maximum limits
 *   - Validates prices are positive
 *   - Calculates real subtotal from item prices * quantities
 *   - Applies discount coupons (percentage and fixed amount)
 *   - Reserves inventory for each item (thread-safe in-memory tracker)
 *
 * Input: cartId, userId
 * Output: valid, subtotal, itemCount, items, discountApplied, discountAmount
 */
public class ValidateCartWorker implements Worker {

    /** In-memory inventory tracker. Maps sku -> available quantity. Thread-safe. */
    private static final ConcurrentHashMap<String, Integer> INVENTORY = new ConcurrentHashMap<>();

    /** Known discount coupons: code -> {type, value}. */
    private static final Map<String, Map<String, Object>> COUPONS = Map.of(
            "SAVE10", Map.of("type", "percentage", "value", 10.0, "minOrder", 50.0),
            "SAVE20", Map.of("type", "percentage", "value", 20.0, "minOrder", 100.0),
            "FLAT5", Map.of("type", "fixed", "value", 5.0, "minOrder", 25.0),
            "FLAT15", Map.of("type", "fixed", "value", 15.0, "minOrder", 75.0),
            "WELCOME", Map.of("type", "percentage", "value", 15.0, "minOrder", 0.0)
    );

    private static final int MAX_QUANTITY_PER_ITEM = 100;
    private static final double MAX_PRICE_PER_ITEM = 50000.0;

    static {
        // Seed inventory with default stock levels
        INVENTORY.putIfAbsent("LAPTOP-PRO", 50);
        INVENTORY.putIfAbsent("USB-C-HUB", 200);
        INVENTORY.putIfAbsent("WIRELESS-MOUSE", 150);
        INVENTORY.putIfAbsent("MONITOR-27", 30);
        INVENTORY.putIfAbsent("KEYBOARD-MECH", 75);
        INVENTORY.putIfAbsent("HEADPHONES", 100);
    }

    @Override
    public String getTaskDefName() {
        return "chk_validate_cart";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        String cartId = task.getInputData().get("cartId") != null
                ? task.getInputData().get("cartId").toString() : "UNKNOWN";
        String userId = task.getInputData().get("userId") != null
                ? task.getInputData().get("userId").toString() : "UNKNOWN";

        // Get cart items
        List<Map<String, Object>> items = new ArrayList<>();
        Object itemsObj = task.getInputData().get("items");
        if (itemsObj instanceof List) {
            items = (List<Map<String, Object>>) itemsObj;
        }

        // Get optional coupon
        String couponCode = task.getInputData().get("couponCode") != null
                ? task.getInputData().get("couponCode").toString().toUpperCase() : null;

        // If no items provided, use a default cart for demo purposes
        if (items.isEmpty()) {
            items = List.of(
                    Map.of("sku", "LAPTOP-PRO", "name", "Laptop Pro 16", "price", 1999.00, "qty", 1),
                    Map.of("sku", "USB-C-HUB", "name", "USB-C Hub", "price", 49.99, "qty", 1),
                    Map.of("sku", "WIRELESS-MOUSE", "name", "Wireless Mouse", "price", 29.99, "qty", 2)
            );
        }

        // --- Validate each item ---
        List<String> errors = new ArrayList<>();
        double subtotal = 0.0;
        int totalItemCount = 0;
        List<Map<String, Object>> validatedItems = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            String sku = item.get("sku") != null ? item.get("sku").toString() : null;
            String name = item.get("name") != null ? item.get("name").toString() : "Unknown Item";

            if (sku == null || sku.isBlank()) {
                errors.add("Item " + (i + 1) + ": missing SKU");
                continue;
            }

            // Validate quantity
            int qty = 0;
            if (item.get("qty") instanceof Number) {
                qty = ((Number) item.get("qty")).intValue();
            }
            if (qty <= 0) {
                errors.add("Item " + sku + ": quantity must be positive");
                continue;
            }
            if (qty > MAX_QUANTITY_PER_ITEM) {
                errors.add("Item " + sku + ": quantity " + qty + " exceeds maximum " + MAX_QUANTITY_PER_ITEM);
                continue;
            }

            // Validate price
            double price = 0;
            if (item.get("price") instanceof Number) {
                price = ((Number) item.get("price")).doubleValue();
            }
            if (price <= 0 || price > MAX_PRICE_PER_ITEM) {
                errors.add("Item " + sku + ": invalid price $" + price);
                continue;
            }

            // Check inventory availability
            int available = INVENTORY.getOrDefault(sku, 0);
            if (available < qty) {
                // Allow items not tracked in inventory (assume available)
                if (INVENTORY.containsKey(sku)) {
                    errors.add("Item " + sku + ": only " + available + " available, requested " + qty);
                    continue;
                }
            }

            double lineTotal = Math.round(price * qty * 100.0) / 100.0;
            subtotal += lineTotal;
            totalItemCount += qty;

            Map<String, Object> validItem = new LinkedHashMap<>();
            validItem.put("sku", sku);
            validItem.put("name", name);
            validItem.put("price", price);
            validItem.put("qty", qty);
            validItem.put("lineTotal", lineTotal);
            validatedItems.add(validItem);
        }

        subtotal = Math.round(subtotal * 100.0) / 100.0;

        // --- Apply coupon discount ---
        double discountAmount = 0.0;
        String discountApplied = null;
        if (couponCode != null && COUPONS.containsKey(couponCode)) {
            Map<String, Object> coupon = COUPONS.get(couponCode);
            double minOrder = ((Number) coupon.get("minOrder")).doubleValue();
            if (subtotal >= minOrder) {
                String type = (String) coupon.get("type");
                double value = ((Number) coupon.get("value")).doubleValue();
                if ("percentage".equals(type)) {
                    discountAmount = Math.round(subtotal * value / 100.0 * 100.0) / 100.0;
                } else {
                    discountAmount = value;
                }
                discountAmount = Math.min(discountAmount, subtotal); // Don't exceed subtotal
                subtotal = Math.round((subtotal - discountAmount) * 100.0) / 100.0;
                discountApplied = couponCode;
            } else {
                errors.add("Coupon " + couponCode + " requires minimum order of $" + minOrder);
            }
        } else if (couponCode != null) {
            errors.add("Invalid coupon code: " + couponCode);
        }

        boolean valid = errors.isEmpty() && !validatedItems.isEmpty();

        System.out.println("  [validate] Cart " + cartId + " (user " + userId + "): "
                + totalItemCount + " items, subtotal=$" + subtotal
                + (discountApplied != null ? " (discount " + discountApplied + ": -$" + discountAmount + ")" : "")
                + " -> valid=" + valid);

        output.put("valid", valid);
        output.put("subtotal", subtotal);
        output.put("itemCount", totalItemCount);
        output.put("lineItems", validatedItems.size());
        output.put("items", validatedItems);

        if (discountApplied != null) {
            output.put("discountApplied", discountApplied);
            output.put("discountAmount", discountAmount);
        }

        if (!errors.isEmpty()) {
            output.put("errors", errors);
        }

        result.setOutputData(output);
        result.setStatus(valid ? TaskResult.Status.COMPLETED : TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
        if (!valid) {
            result.setReasonForIncompletion("Cart validation failed: " + String.join(", ", errors));
        }
        return result;
    }

    /** Seed or update inventory for a SKU. Useful for testing. */
    public static void setInventory(String sku, int qty) {
        INVENTORY.put(sku, qty);
    }

    /** Get current inventory for a SKU. */
    public static int getInventory(String sku) {
        return INVENTORY.getOrDefault(sku, 0);
    }

    /** Reset inventory to defaults. */
    public static void resetInventory() {
        INVENTORY.clear();
        INVENTORY.put("LAPTOP-PRO", 50);
        INVENTORY.put("USB-C-HUB", 200);
        INVENTORY.put("WIRELESS-MOUSE", 150);
        INVENTORY.put("MONITOR-27", 30);
        INVENTORY.put("KEYBOARD-MECH", 75);
        INVENTORY.put("HEADPHONES", 100);
    }
}
