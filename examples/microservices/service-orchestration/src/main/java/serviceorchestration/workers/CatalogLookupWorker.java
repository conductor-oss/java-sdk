package serviceorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Looks up a product in the catalog.
 * Input: productId, authToken
 * Output: product (id, name, price, available)
 */
public class CatalogLookupWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "so_catalog_lookup";
    }

    @Override
    public TaskResult execute(Task task) {
        String productId = (String) task.getInputData().get("productId");
        if (productId == null) {
            productId = "UNKNOWN";
        }

        System.out.println("  [so_catalog_lookup] Looking up product " + productId + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("product", Map.of(
                "id", productId,
                "name", "Wireless Headphones",
                "price", 79.99,
                "available", true
        ));
        return result;
    }
}
