package ordermanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fulfills an order: transitions CONFIRMED -> PROCESSING.
 *
 * Real fulfillment logic:
 *   - Validates the order is in CONFIRMED state
 *   - Generates a fulfillment ID with warehouse prefix
 *   - Performs pick & pack by processing each item
 *   - Transitions order state via state machine
 *   - Records fulfillment timestamps
 *
 * Input: orderId, items, warehouseId
 * Output: fulfillmentId, warehouseId, packedAt, itemsFulfilled, status
 */
public class FulfillOrderWorker implements Worker {

    private static final AtomicLong COUNTER = new AtomicLong();

    @Override
    public String getTaskDefName() {
        return "ord_fulfill";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        String orderId = task.getInputData().get("orderId") != null
                ? task.getInputData().get("orderId").toString() : "UNKNOWN";
        String warehouseId = task.getInputData().get("warehouseId") != null
                ? task.getInputData().get("warehouseId").toString() : "WH-EAST-01";

        List<Map<String, Object>> items = new ArrayList<>();
        Object itemsObj = task.getInputData().get("items");
        if (itemsObj instanceof List) {
            items = (List<Map<String, Object>>) itemsObj;
        }

        // Validate state transition
        String previousStatus = OrderStore.getStatus(orderId);
        boolean canFulfill = OrderStore.isValidTransition(orderId, "PROCESSING");

        if (!canFulfill && previousStatus != null) {
            output.put("error", "Cannot fulfill: order is in " + previousStatus + " state");
            output.put("status", previousStatus);
            result.setOutputData(output);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Invalid state transition from " + previousStatus + " to PROCESSING");
            return result;
        }

        // Generate fulfillment ID
        String fulfillmentId = "FUL-" + warehouseId.replace("WH-", "") + "-"
                + Long.toString(System.currentTimeMillis(), 36) + "-" + COUNTER.incrementAndGet();

        Instant now = Instant.now();

        // Process each item (pick & pack processing with real item tracking)
        List<Map<String, Object>> fulfilledItems = new ArrayList<>();
        int totalUnits = 0;
        for (Map<String, Object> item : items) {
            String sku = item.get("sku") != null ? item.get("sku").toString() : "UNKNOWN";
            int qty = item.get("qty") instanceof Number ? ((Number) item.get("qty")).intValue() : 0;
            totalUnits += qty;

            Map<String, Object> fulfilled = new LinkedHashMap<>();
            fulfilled.put("sku", sku);
            fulfilled.put("qty", qty);
            fulfilled.put("pickedAt", now.toString());
            fulfilled.put("bin", "BIN-" + Math.abs(sku.hashCode() % 100));
            fulfilledItems.add(fulfilled);
        }

        // Transition state
        boolean transitioned = OrderStore.transition(orderId, "PROCESSING", "fulfill_worker",
                "Fulfillment " + fulfillmentId + " started at " + warehouseId);

        String currentStatus = OrderStore.getStatus(orderId);

        System.out.println("  [fulfill] Order " + orderId + ": picked & packed " + totalUnits
                + " units at " + warehouseId + " -> " + fulfillmentId
                + " (" + previousStatus + " -> " + currentStatus + ")");

        output.put("fulfillmentId", fulfillmentId);
        output.put("warehouseId", warehouseId);
        output.put("packedAt", now.toString());
        output.put("itemsFulfilled", fulfilledItems);
        output.put("totalUnits", totalUnits);
        output.put("previousStatus", previousStatus);
        output.put("status", currentStatus != null ? currentStatus : "PROCESSING");

        result.setOutputData(output);
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
