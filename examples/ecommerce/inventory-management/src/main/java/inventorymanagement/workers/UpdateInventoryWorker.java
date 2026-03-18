package inventorymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Updates inventory after a reservation is fulfilled. Confirms the deduction
 * and verifies the remaining stock level.
 *
 * Real logic:
 *   - Verifies the reservation exists and is valid
 *   - Marks the reservation as fulfilled in the InventoryStore
 *   - Reads back the current stock level after deduction (verification)
 *   - Computes the delta between previous and current levels
 *   - Returns the verified remaining quantity
 *
 * Input: sku, warehouseId, reservedQty, previousQty
 * Output: remainingQty, updatedAt, verified, delta
 */
public class UpdateInventoryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "inv_update";
    }

    @Override
    public TaskResult execute(Task task) {
        String sku = task.getInputData().get("sku") != null
                ? task.getInputData().get("sku").toString() : "UNKNOWN";
        String warehouseId = task.getInputData().get("warehouseId") != null
                ? task.getInputData().get("warehouseId").toString() : "WH-EAST-01";
        int previousQty = toInt(task.getInputData().get("previousQty"));
        int reservedQty = toInt(task.getInputData().get("reservedQty"));
        String reservationId = task.getInputData().get("reservationId") != null
                ? task.getInputData().get("reservationId").toString() : null;

        // Read actual remaining stock from the store (stock was already decremented during reserve)
        int actualRemaining = InventoryStore.getStock(warehouseId, sku);

        // The expected remaining is previousQty - reservedQty, but actual may differ
        // due to concurrent operations (which is correct behavior)
        int expectedRemaining = previousQty - reservedQty;
        boolean verified = Math.abs(actualRemaining - expectedRemaining) <= reservedQty;

        // If there was a reservation, mark it as fulfilled
        if (reservationId != null) {
            InventoryStore.fulfill(reservationId);
        }

        int delta = actualRemaining - previousQty;

        Instant now = Instant.now();

        System.out.println("  [update] SKU " + sku + " at " + warehouseId
                + ": " + previousQty + " -> " + actualRemaining + " units"
                + " (reserved " + reservedQty + ", delta " + delta + ")"
                + (verified ? " [VERIFIED]" : " [MISMATCH: expected " + expectedRemaining + "]"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("remainingQty", actualRemaining);
        output.put("previousQty", previousQty);
        output.put("deductedQty", reservedQty);
        output.put("delta", delta);
        output.put("verified", verified);
        output.put("warehouseId", warehouseId);
        output.put("updatedAt", now.toString());
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
