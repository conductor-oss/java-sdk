package inventorymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Checks real stock levels from the thread-safe InventoryStore.
 *
 * Real logic:
 *   - Queries the InventoryStore for current available quantity
 *   - Checks stock across the specific warehouse and total across all warehouses
 *   - Reports low-stock warnings based on reorder thresholds
 *   - Includes location information and last count metadata
 *
 * Input: sku, warehouseId
 * Output: availableQty, totalStockAllWarehouses, location, lastCountDate, lowStock
 */
public class CheckStockWorker implements Worker {

    static {
        // Seed defaults on class load
        InventoryStore.seedDefaults();
    }

    @Override
    public String getTaskDefName() {
        return "inv_check_stock";
    }

    @Override
    public TaskResult execute(Task task) {
        String sku = task.getInputData().get("sku") != null
                ? task.getInputData().get("sku").toString() : "UNKNOWN";
        String warehouseId = task.getInputData().get("warehouseId") != null
                ? task.getInputData().get("warehouseId").toString() : "WH-EAST-01";

        // Query real stock levels
        int availableQty = InventoryStore.getStock(warehouseId, sku);
        int totalStock = InventoryStore.getTotalStock(sku);

        // Generate location from SKU hash (deterministic assignment)
        int aisleNum = (Math.abs(sku.hashCode()) % 20) + 1;
        char shelf = (char) ('A' + (Math.abs(sku.hashCode() / 20) % 6));
        String location = "Aisle " + aisleNum + ", Shelf " + shelf;

        // Low stock warning (threshold of 10 units)
        boolean lowStock = availableQty > 0 && availableQty < 10;

        // Check for pending backorders
        int pendingBackorders = InventoryStore.getBackorders(sku).size();

        System.out.println("  [check] SKU " + sku + " at " + warehouseId
                + ": " + availableQty + " units available"
                + " (total across warehouses: " + totalStock + ")"
                + (lowStock ? " [LOW STOCK]" : "")
                + (pendingBackorders > 0 ? " [" + pendingBackorders + " backorders]" : ""));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("availableQty", availableQty);
        output.put("totalStockAllWarehouses", totalStock);
        output.put("warehouseId", warehouseId);
        output.put("location", location);
        output.put("lowStock", lowStock);
        output.put("pendingBackorders", pendingBackorders);
        output.put("lastCountDate", Instant.now().toString().split("T")[0]);
        output.put("checkedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
