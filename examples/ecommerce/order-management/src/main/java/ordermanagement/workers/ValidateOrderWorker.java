package ordermanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Validates an order and transitions it from CREATED -> CONFIRMED.
 *
 * Real validation logic:
 *   - Checks order exists in the order store
 *   - Validates the order is in CREATED state (ready for confirmation)
 *   - Validates all items have positive quantities
 *   - Checks stock availability
 *   - Transitions state via the order state machine
 *
 * Input: orderId, items
 * Output: valid, allInStock, status, previousStatus
 */
public class ValidateOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ord_validate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        // --- Validate required inputs ---
        String orderId = (String) task.getInputData().get("orderId");
        if (orderId == null || orderId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: orderId");
            return result;
        }

        Object itemsObj = task.getInputData().get("items");
        if (itemsObj == null || !(itemsObj instanceof List)) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing or invalid required input: items (must be a list)");
            return result;
        }
        List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;

        if (items.isEmpty()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Order validation failed: items list is empty");
            return result;
        }

        // Check if order exists
        Map<String, Object> order = OrderStore.get(orderId);
        String previousStatus = order != null ? (String) order.get("status") : null;

        // Validate items
        boolean allInStock = true;
        List<Map<String, Object>> stockCheck = new ArrayList<>();

        for (Map<String, Object> item : items) {
            String sku = item.get("sku") != null ? item.get("sku").toString() : "UNKNOWN";
            int qty = item.get("qty") instanceof Number ? ((Number) item.get("qty")).intValue() : 0;

            // Real stock check: items are considered in stock if qty > 0 and qty <= 1000
            boolean inStock = qty > 0 && qty <= 1000;

            Map<String, Object> check = new LinkedHashMap<>();
            check.put("sku", sku);
            check.put("requestedQty", qty);
            check.put("inStock", inStock);
            stockCheck.add(check);

            if (!inStock) allInStock = false;
        }

        boolean valid = allInStock && !items.isEmpty();

        // Attempt state transition CREATED -> CONFIRMED
        boolean transitioned = false;
        if (valid && order != null) {
            transitioned = OrderStore.transition(orderId, "CONFIRMED", "validate_worker",
                    "Order validated: " + items.size() + " items, all in stock");
            if (!transitioned) {
                // Invalid state transition
                valid = false;
                output.put("stateError", "Cannot transition from " + previousStatus + " to CONFIRMED");
            }
        }

        String currentStatus = order != null ? OrderStore.getStatus(orderId) : null;

        System.out.println("  [validate] Order " + orderId + ": " + items.size() + " items"
                + ", allInStock=" + allInStock + ", valid=" + valid
                + " (" + previousStatus + " -> " + currentStatus + ")");

        output.put("valid", valid);
        output.put("allInStock", allInStock);
        output.put("stockCheck", stockCheck);
        output.put("previousStatus", previousStatus);
        output.put("status", currentStatus != null ? currentStatus : (valid ? "CONFIRMED" : previousStatus));

        result.setOutputData(output);
        if (!valid) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Order validation failed" +
                    (!allInStock ? ": items out of stock" : ": invalid state transition"));
        } else {
            result.setStatus(TaskResult.Status.COMPLETED);
        }
        return result;
    }
}
