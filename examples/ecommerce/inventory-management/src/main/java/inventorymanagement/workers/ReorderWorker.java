package inventorymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Evaluates whether a reorder is needed and generates a purchase order.
 *
 * Real reorder logic:
 *   - Compares remaining quantity against the reorder threshold
 *   - Calculates reorder quantity using Economic Order Quantity (EOQ) heuristic:
 *     reorderQty = max(threshold * 2, 50) (ensures at least 50 units or 2x threshold)
 *   - Considers pending backorders in the reorder calculation
 *   - Generates a purchase order number
 *   - Calculates estimated arrival date (14 days for standard, 7 for rush)
 *
 * Input: sku, remainingQty, reorderThreshold
 * Output: reorderPlaced, reorderQty, poNumber, estimatedArrival, backordersCleared
 */
public class ReorderWorker implements Worker {

    private static final AtomicLong COUNTER = new AtomicLong();
    private static final int MIN_REORDER_QTY = 50;

    @Override
    public String getTaskDefName() {
        return "inv_reorder";
    }

    @Override
    public TaskResult execute(Task task) {
        String sku = task.getInputData().get("sku") != null
                ? task.getInputData().get("sku").toString() : "UNKNOWN";
        int remaining = toInt(task.getInputData().get("remainingQty"));
        int threshold = toInt(task.getInputData().get("reorderThreshold"));

        // Check if reorder is needed
        boolean reorderPlaced = remaining <= threshold;

        // Check pending backorders
        List<Map<String, Object>> backorders = InventoryStore.getBackorders(sku);
        int backorderQty = backorders.stream()
                .mapToInt(bo -> bo.get("qty") instanceof Number ? ((Number) bo.get("qty")).intValue() : 0)
                .sum();

        // Calculate reorder quantity using EOQ heuristic
        int reorderQty = 0;
        String poNumber = null;
        String estimatedArrival = null;
        String priority = "standard";

        if (reorderPlaced) {
            // Base: max(threshold * 2, MIN_REORDER_QTY)
            reorderQty = Math.max(threshold * 2, MIN_REORDER_QTY);

            // Add backorder quantity to ensure we can fulfill waiting orders
            reorderQty += backorderQty;

            // If stock is zero or negative, mark as rush
            if (remaining <= 0) {
                priority = "rush";
            }

            // Generate PO number
            poNumber = "PO-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase()
                    + "-" + COUNTER.incrementAndGet();

            // Calculate estimated arrival
            int leadDays = "rush".equals(priority) ? 7 : 14;
            estimatedArrival = Instant.now().plus(leadDays, ChronoUnit.DAYS)
                    .toString().split("T")[0];

            System.out.println("  [reorder] SKU " + sku + ": " + remaining + " <= " + threshold
                    + " threshold -> reorder " + reorderQty + " units"
                    + " (" + priority + ", PO: " + poNumber
                    + ", ETA: " + estimatedArrival + ")"
                    + (backorderQty > 0 ? " [includes " + backorderQty + " backorder units]" : ""));
        } else {
            System.out.println("  [reorder] SKU " + sku + ": " + remaining + " > " + threshold
                    + " threshold -> no reorder needed"
                    + (backorderQty > 0 ? " [" + backorderQty + " units backordered]" : ""));
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("reorderPlaced", reorderPlaced);
        output.put("reorderQty", reorderQty);
        output.put("poNumber", poNumber);
        output.put("priority", reorderPlaced ? priority : null);
        output.put("estimatedArrival", estimatedArrival);
        output.put("currentStock", remaining);
        output.put("reorderThreshold", threshold);
        output.put("pendingBackorders", backorderQty);
        result.setOutputData(output);
        return result;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try { return Integer.parseInt((String) obj); } catch (NumberFormatException e) { /* ignore */ }
        }
        return 0;
    }
}
