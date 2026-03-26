package forkjoin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merges product, inventory, and review data into a single product page.
 * Runs after the FORK_JOIN completes, combining all three parallel outputs.
 */
public class MergeResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fj_merge_results";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> product = (Map<String, Object>) task.getInputData().get("product");
        Map<String, Object> inventory = (Map<String, Object>) task.getInputData().get("inventory");
        Map<String, Object> reviews = (Map<String, Object>) task.getInputData().get("reviews");

        if (product == null) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'product' is required — the product fork branch must complete successfully");
            return fail;
        }
        if (inventory == null) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'inventory' is required — the inventory fork branch must complete successfully");
            return fail;
        }
        if (reviews == null) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'reviews' is required — the reviews fork branch must complete successfully");
            return fail;
        }

        String name = (String) product.get("name");
        Object priceObj = product.get("price");
        double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0.0;

        Object inStockObj = inventory.get("inStock");
        boolean available = inStockObj instanceof Boolean && (Boolean) inStockObj;
        Object qtyObj = inventory.get("quantity");
        int stock = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 0;

        Object ratingObj = reviews.get("averageRating");
        double rating = ratingObj instanceof Number ? ((Number) ratingObj).doubleValue() : 0.0;
        Object countObj = reviews.get("totalReviews");
        int reviewCount = countObj instanceof Number ? ((Number) countObj).intValue() : 0;

        System.out.println("  [fj_merge_results] Building product page:");
        System.out.println("    -> " + name + " | $" + price + " | " +
                (available ? "In Stock (" + stock + ")" : "Out of Stock") +
                " | " + rating + " stars (" + reviewCount + " reviews)");

        Map<String, Object> productPage = new LinkedHashMap<>();
        productPage.put("name", name);
        productPage.put("price", price);
        productPage.put("available", available);
        productPage.put("stock", stock);
        productPage.put("rating", rating);
        productPage.put("reviewCount", reviewCount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("productPage", productPage);
        return result;
    }
}
