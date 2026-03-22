package ordermanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Creates a new order with real state machine tracking.
 *
 * State machine: CREATED -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED
 *                (CANCELLED can be reached from any state)
 *
 * Real logic:
 *   - Validates items have required fields (sku, price, qty)
 *   - Calculates real totals from item prices * quantities
 *   - Initializes order in CREATED state with full history
 *   - Stores order in a shared in-memory order store (thread-safe)
 *
 * Input: customerId, items
 * Output: orderId, total, status, createdAt
 */
public class CreateOrderWorker implements Worker {

    private static final AtomicLong COUNTER = new AtomicLong();

    @Override
    public String getTaskDefName() {
        return "ord_create";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        // --- Validate required inputs ---
        String customerId = (String) task.getInputData().get("customerId");
        if (customerId == null || customerId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: customerId");
            return result;
        }

        Object itemsObj = task.getInputData().get("items");
        if (itemsObj == null || !(itemsObj instanceof List)) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing or invalid required input: items (must be a list)");
            return result;
        }
        List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;

        // Validate and calculate total
        double total = 0;
        List<Map<String, Object>> validatedItems = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            String sku = item.get("sku") != null ? item.get("sku").toString() : null;
            if (sku == null || sku.isBlank()) {
                errors.add("Item " + (i + 1) + ": missing SKU");
                continue;
            }

            double price = item.get("price") instanceof Number ? ((Number) item.get("price")).doubleValue() : 0;
            int qty = item.get("qty") instanceof Number ? ((Number) item.get("qty")).intValue() : 0;

            if (price <= 0) {
                errors.add("Item " + sku + ": invalid price (" + price + ")");
                continue;
            }
            if (qty <= 0) {
                errors.add("Item " + sku + ": invalid quantity (" + qty + ")");
                continue;
            }

            double lineTotal = Math.round(price * qty * 100.0) / 100.0;
            total += lineTotal;

            Map<String, Object> validItem = new LinkedHashMap<>();
            validItem.put("sku", sku);
            validItem.put("name", item.getOrDefault("name", sku));
            validItem.put("price", price);
            validItem.put("qty", qty);
            validItem.put("lineTotal", lineTotal);
            validatedItems.add(validItem);
        }

        total = Math.round(total * 100.0) / 100.0;

        if (validatedItems.isEmpty()) {
            output.put("error", "No valid items in order");
            if (!errors.isEmpty()) output.put("validationErrors", errors);
            result.setOutputData(output);
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Order must contain at least one valid item. Errors: " + String.join("; ", errors));
            return result;
        }

        // Generate order ID
        String orderId = "ORD-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase()
                + "-" + COUNTER.incrementAndGet();

        Instant now = Instant.now();

        // Create state history entry
        Map<String, Object> historyEntry = new LinkedHashMap<>();
        historyEntry.put("state", "CREATED");
        historyEntry.put("timestamp", now.toString());
        historyEntry.put("actor", "system");
        historyEntry.put("note", "Order created with " + validatedItems.size() + " items");

        // Store order in the shared order store
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderId", orderId);
        order.put("customerId", customerId);
        order.put("status", "CREATED");
        order.put("items", validatedItems);
        order.put("total", total);
        order.put("createdAt", now.toString());
        order.put("history", Collections.synchronizedList(new ArrayList<>(List.of(historyEntry))));
        OrderStore.put(orderId, order);

        System.out.printf("  [create] Order %s: %d items, total=$%.2f, customer=%s%n",
                orderId, validatedItems.size(), total, customerId);

        output.put("orderId", orderId);
        output.put("total", String.format("%.2f", total));
        output.put("itemCount", validatedItems.size());
        output.put("status", "CREATED");
        output.put("createdAt", now.toString());

        if (!errors.isEmpty()) {
            output.put("warnings", errors);
        }

        result.setOutputData(output);
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
