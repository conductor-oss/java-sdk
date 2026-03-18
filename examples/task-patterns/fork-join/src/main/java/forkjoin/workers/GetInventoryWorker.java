package forkjoin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fetches inventory information for a given product ID.
 * Computes stock levels based on the product ID hash,
 * representing inventory lookup results.
 */
public class GetInventoryWorker implements Worker {

    private static final String[] WAREHOUSES = {
            "US-WEST-1", "US-WEST-2", "US-EAST-1", "EU-WEST-1", "AP-SOUTH-1"
    };

    @Override
    public String getTaskDefName() {
        return "fj_get_inventory";
    }

    @Override
    public TaskResult execute(Task task) {
        String productId = (String) task.getInputData().get("productId");
        if (productId == null || productId.isBlank()) {
            productId = "UNKNOWN";
        }

        System.out.println("  [fj_get_inventory] Checking inventory for: " + productId);

        // Deterministic inventory data based on product ID
        int hash = Math.abs(productId.hashCode());
        int quantity = hash % 500;
        boolean inStock = quantity > 0;
        String warehouse = WAREHOUSES[hash % WAREHOUSES.length];

        Map<String, Object> inventory = new LinkedHashMap<>();
        inventory.put("productId", productId);
        inventory.put("inStock", inStock);
        inventory.put("quantity", quantity);
        inventory.put("warehouse", warehouse);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("inventory", inventory);
        return result;
    }
}
