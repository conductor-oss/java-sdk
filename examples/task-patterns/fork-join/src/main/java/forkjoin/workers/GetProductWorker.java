package forkjoin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fetches product details for a given product ID.
 * Uses a built-in catalog; unknown products get a deterministic fallback
 * derived from the product ID hash.
 */
public class GetProductWorker implements Worker {

    private static final Map<String, Map<String, Object>> CATALOG = Map.of(
            "PROD-001", Map.of("name", "Wireless Headphones", "price", 79.99, "category", "Electronics"),
            "PROD-002", Map.of("name", "Mechanical Keyboard", "price", 129.99, "category", "Electronics"),
            "PROD-003", Map.of("name", "USB-C Hub", "price", 49.99, "category", "Accessories"),
            "PROD-004", Map.of("name", "Standing Desk", "price", 399.99, "category", "Furniture"),
            "PROD-005", Map.of("name", "Noise Cancelling Earbuds", "price", 149.99, "category", "Electronics")
    );

    @Override
    public String getTaskDefName() {
        return "fj_get_product";
    }

    @Override
    public TaskResult execute(Task task) {
        String productId = (String) task.getInputData().get("productId");
        if (productId == null || productId.isBlank()) {
            productId = "UNKNOWN";
        }

        System.out.println("  [fj_get_product] Fetching product details for: " + productId);

        Map<String, Object> product = new LinkedHashMap<>();
        product.put("id", productId);

        Map<String, Object> catalogEntry = CATALOG.get(productId);
        if (catalogEntry != null) {
            product.put("name", catalogEntry.get("name"));
            product.put("price", catalogEntry.get("price"));
            product.put("category", catalogEntry.get("category"));
        } else {
            // Deterministic fallback based on product ID hash
            int hash = Math.abs(productId.hashCode());
            double price = 10.0 + (hash % 990);
            product.put("name", "Product " + productId);
            product.put("price", price);
            product.put("category", "General");
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("product", product);
        return result;
    }
}
