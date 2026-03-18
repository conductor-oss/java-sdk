package choreographyvsorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReserveInventoryWorker implements Worker {

    // In-memory warehouse inventory: item -> available quantity
    private static final ConcurrentHashMap<String, Integer> INVENTORY = new ConcurrentHashMap<>();
    private static final String[] WAREHOUSES = {"WH-EAST", "WH-WEST", "WH-CENTRAL"};

    static {
        // Seed default inventory for common items
        INVENTORY.put("widget", 500);
        INVENTORY.put("gadget", 200);
        INVENTORY.put("gizmo", 150);
        INVENTORY.put("doohickey", 75);
        INVENTORY.put("thingamajig", 300);
    }

    /** Expose inventory for testing. */
    public static ConcurrentHashMap<String, Integer> getInventory() {
        return INVENTORY;
    }

    @Override
    public String getTaskDefName() {
        return "cvo_reserve_inventory";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String orderId = (String) task.getInputData().getOrDefault("orderId", "unknown");
        Object itemsObj = task.getInputData().get("items");

        if (itemsObj == null) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("No items provided to reserve");
            return r;
        }

        List<?> items;
        if (itemsObj instanceof List) {
            items = (List<?>) itemsObj;
        } else {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("items must be a list");
            return r;
        }

        // Attempt to reserve each item
        List<Map<String, Object>> reservations = new ArrayList<>();
        List<String> insufficientItems = new ArrayList<>();

        for (Object item : items) {
            String itemName;
            int requestedQty;

            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) item;
                itemName = String.valueOf(itemMap.getOrDefault("name", itemMap.getOrDefault("item", "unknown")));
                requestedQty = toInt(itemMap.getOrDefault("quantity", 1));
            } else {
                itemName = String.valueOf(item).toLowerCase();
                requestedQty = 1;
            }

            if (!INVENTORY.containsKey(itemName)) {
                insufficientItems.add(itemName + " (not in inventory)");
                reservations.add(Map.of("item", itemName, "requested", requestedQty, "reserved", false));
                continue;
            }

            // Attempt atomic reservation using CAS loop
            final int reqQty = requestedQty;
            AtomicBoolean success = new AtomicBoolean(false);
            INVENTORY.computeIfPresent(itemName, (key, available) -> {
                if (available >= reqQty) {
                    success.set(true);
                    return available - reqQty;
                }
                return available; // insufficient, don't modify
            });

            if (success.get()) {
                String warehouse = WAREHOUSES[Math.abs(itemName.hashCode()) % WAREHOUSES.length];
                reservations.add(Map.of("item", itemName, "requested", requestedQty,
                        "reserved", true, "warehouse", warehouse));
            } else {
                int available = INVENTORY.getOrDefault(itemName, 0);
                insufficientItems.add(itemName + " (requested " + requestedQty + ", available " + available + ")");
                reservations.add(Map.of("item", itemName, "requested", requestedQty,
                        "reserved", false, "available", available));
            }
        }

        if (!insufficientItems.isEmpty()) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Insufficient inventory: " + String.join(", ", insufficientItems));
            r.getOutputData().put("reserved", false);
            r.getOutputData().put("reservations", reservations);
            return r;
        }

        // Determine primary warehouse (most items assigned to)
        String primaryWarehouse = reservations.stream()
                .filter(res -> Boolean.TRUE.equals(res.get("reserved")))
                .map(res -> (String) res.get("warehouse"))
                .reduce((a, b) -> a) // pick first warehouse
                .orElse("WH-EAST");

        System.out.println("  [inventory] Reserved " + reservations.size() + " items for order "
                + orderId + " at " + primaryWarehouse);

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reserved", true);
        r.getOutputData().put("warehouse", primaryWarehouse);
        r.getOutputData().put("reservations", reservations);
        r.getOutputData().put("orderId", orderId);
        return r;
    }

    private static int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(String.valueOf(obj)); } catch (Exception e) { return 1; }
    }
}
