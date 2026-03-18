package inventorymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reserves stock using CAS (compare-and-swap) for thread safety.
 *
 * Real logic:
 *   - Uses InventoryStore.reserve() which performs atomic CAS operations
 *   - If full quantity is not available, reserves as much as possible
 *   - Creates a backorder for any unfulfilled quantity
 *   - Returns reservation ID for later fulfillment or release
 *   - Handles concurrent reservation attempts safely
 *
 * Input: sku, requestedQty, availableQty (informational, actual check done atomically)
 * Output: reserved, reservedQty, reservationId, backordered, backorderQty
 */
public class ReserveStockWorker implements Worker {

    private static final AtomicLong COUNTER = new AtomicLong();

    @Override
    public String getTaskDefName() {
        return "inv_reserve";
    }

    @Override
    public TaskResult execute(Task task) {
        String sku = task.getInputData().get("sku") != null
                ? task.getInputData().get("sku").toString() : "UNKNOWN";
        int requested = toInt(task.getInputData().get("requestedQty"));
        // availableQty from check step (informational, we re-check atomically)
        int reportedAvailable = toInt(task.getInputData().get("availableQty"));

        // Determine warehouse (default)
        String warehouseId = task.getInputData().get("warehouseId") != null
                ? task.getInputData().get("warehouseId").toString() : "WH-EAST-01";

        // Generate reservation ID
        String reservationId = "res-" + Long.toString(System.currentTimeMillis(), 36)
                + "-" + COUNTER.incrementAndGet();

        // Attempt atomic reservation
        int reserved = 0;
        boolean success = false;
        String actualReservationId = null;

        if (requested > 0) {
            // Try to reserve the full amount first
            actualReservationId = InventoryStore.reserve(warehouseId, sku, requested, reservationId);

            if (actualReservationId != null) {
                // Full reservation succeeded
                reserved = requested;
                success = true;
            } else {
                // Full amount not available; try to reserve what's available
                int currentStock = InventoryStore.getStock(warehouseId, sku);
                if (currentStock > 0) {
                    int toReserve = Math.min(requested, currentStock);
                    actualReservationId = InventoryStore.reserve(warehouseId, sku, toReserve, reservationId);
                    if (actualReservationId != null) {
                        reserved = toReserve;
                        success = true;
                    }
                }
            }
        }

        // Create backorder for unfulfilled quantity
        int backorderQty = requested - reserved;
        boolean backordered = backorderQty > 0;
        if (backordered) {
            InventoryStore.addBackorder(sku, backorderQty, reservationId);
        }

        System.out.println("  [reserve] SKU " + sku + " at " + warehouseId
                + ": requested=" + requested + ", reserved=" + reserved
                + (backordered ? ", backordered=" + backorderQty : "")
                + " -> " + (success ? reservationId : "FAILED"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("reserved", success);
        output.put("reservedQty", reserved);
        output.put("reservationId", success ? reservationId : null);
        output.put("requestedQty", requested);
        output.put("warehouseId", warehouseId);
        output.put("backordered", backordered);
        output.put("backorderQty", backorderQty);
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
