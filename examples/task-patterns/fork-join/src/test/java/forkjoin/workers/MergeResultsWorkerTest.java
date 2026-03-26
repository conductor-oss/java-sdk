package forkjoin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergeResultsWorkerTest {

    private final MergeResultsWorker worker = new MergeResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("fj_merge_results", worker.getTaskDefName());
    }

    @Test
    void mergesAllDataIntoProductPage() {
        Map<String, Object> product = Map.of(
                "id", "PROD-001", "name", "Wireless Headphones", "price", 79.99, "category", "Electronics");
        Map<String, Object> inventory = Map.of(
                "productId", "PROD-001", "inStock", true, "quantity", 142, "warehouse", "US-WEST-2");
        Map<String, Object> reviews = Map.of(
                "productId", "PROD-001", "averageRating", 4.5, "totalReviews", 328,
                "topReview", "Great sound quality and comfortable fit!");

        Task task = taskWith(Map.of("product", product, "inventory", inventory, "reviews", reviews));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> page = (Map<String, Object>) result.getOutputData().get("productPage");
        assertNotNull(page);
        assertEquals("Wireless Headphones", page.get("name"));
        assertEquals(79.99, page.get("price"));
        assertEquals(true, page.get("available"));
        assertEquals(142, page.get("stock"));
        assertEquals(4.5, page.get("rating"));
        assertEquals(328, page.get("reviewCount"));
    }

    @Test
    void failsWhenProductBranchFailed() {
        Map<String, Object> inventory = Map.of("inStock", true, "quantity", 10);
        Map<String, Object> reviews = Map.of("averageRating", 3.0, "totalReviews", 5);

        Map<String, Object> input = new HashMap<>();
        input.put("product", null);
        input.put("inventory", inventory);
        input.put("reviews", reviews);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("product"));
    }

    @Test
    void failsWhenInventoryBranchFailed() {
        Map<String, Object> product = Map.of("name", "Widget", "price", 9.99);
        Map<String, Object> reviews = Map.of("averageRating", 4.0, "totalReviews", 50);

        Map<String, Object> input = new HashMap<>();
        input.put("product", product);
        input.put("inventory", null);
        input.put("reviews", reviews);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("inventory"));
    }

    @Test
    void failsWhenReviewsBranchFailed() {
        Map<String, Object> product = Map.of("name", "Widget", "price", 9.99);
        Map<String, Object> inventory = Map.of("inStock", false, "quantity", 0);

        Map<String, Object> input = new HashMap<>();
        input.put("product", product);
        input.put("inventory", inventory);
        input.put("reviews", null);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("reviews"));
    }

    @Test
    void failsWhenAllBranchesFailed() {
        Map<String, Object> input = new HashMap<>();
        input.put("product", null);
        input.put("inventory", null);
        input.put("reviews", null);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void failsWhenInputKeysAreMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void handlesOutOfStockInventory() {
        Map<String, Object> product = Map.of("name", "Sold Out Item", "price", 49.99);
        Map<String, Object> inventory = Map.of("inStock", false, "quantity", 0);
        Map<String, Object> reviews = Map.of("averageRating", 2.0, "totalReviews", 3);

        Task task = taskWith(Map.of("product", product, "inventory", inventory, "reviews", reviews));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> page = (Map<String, Object>) result.getOutputData().get("productPage");
        assertEquals(false, page.get("available"));
        assertEquals(0, page.get("stock"));
    }

    @Test
    void productPageContainsAllExpectedFields() {
        Map<String, Object> product = Map.of("name", "Test", "price", 10.0);
        Map<String, Object> inventory = Map.of("inStock", true, "quantity", 5);
        Map<String, Object> reviews = Map.of("averageRating", 3.5, "totalReviews", 12);

        Task task = taskWith(Map.of("product", product, "inventory", inventory, "reviews", reviews));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> page = (Map<String, Object>) result.getOutputData().get("productPage");
        assertEquals(6, page.size());
        assertTrue(page.containsKey("name"));
        assertTrue(page.containsKey("price"));
        assertTrue(page.containsKey("available"));
        assertTrue(page.containsKey("stock"));
        assertTrue(page.containsKey("rating"));
        assertTrue(page.containsKey("reviewCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
