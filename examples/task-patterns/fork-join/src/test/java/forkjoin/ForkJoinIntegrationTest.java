package forkjoin;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import forkjoin.workers.GetInventoryWorker;
import forkjoin.workers.GetProductWorker;
import forkjoin.workers.GetReviewsWorker;
import forkjoin.workers.MergeResultsWorker;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying worker-to-worker data flow in the fork-join pipeline.
 * Simulates parallel execution and verifies merge output contains all branch results.
 */
class ForkJoinIntegrationTest {

    @Test
    void allThreeBranchesFeedIntoMerge() {
        String productId = "PROD-001";

        // Simulate three parallel fork branches
        GetProductWorker productWorker = new GetProductWorker();
        GetInventoryWorker inventoryWorker = new GetInventoryWorker();
        GetReviewsWorker reviewsWorker = new GetReviewsWorker();

        TaskResult productResult = productWorker.execute(taskWith(Map.of("productId", productId)));
        TaskResult inventoryResult = inventoryWorker.execute(taskWith(Map.of("productId", productId)));
        TaskResult reviewsResult = reviewsWorker.execute(taskWith(Map.of("productId", productId)));

        // All three branches must succeed independently
        assertEquals(TaskResult.Status.COMPLETED, productResult.getStatus());
        assertEquals(TaskResult.Status.COMPLETED, inventoryResult.getStatus());
        assertEquals(TaskResult.Status.COMPLETED, reviewsResult.getStatus());

        // Feed results into merge worker
        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) productResult.getOutputData().get("product");
        @SuppressWarnings("unchecked")
        Map<String, Object> inventory = (Map<String, Object>) inventoryResult.getOutputData().get("inventory");
        @SuppressWarnings("unchecked")
        Map<String, Object> reviews = (Map<String, Object>) reviewsResult.getOutputData().get("reviews");

        MergeResultsWorker mergeWorker = new MergeResultsWorker();
        TaskResult mergeResult = mergeWorker.execute(
                taskWith(Map.of("product", product, "inventory", inventory, "reviews", reviews)));

        assertEquals(TaskResult.Status.COMPLETED, mergeResult.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> page = (Map<String, Object>) mergeResult.getOutputData().get("productPage");

        // Verify merge output contains data from ALL three branches
        assertEquals("Wireless Headphones", page.get("name")); // from product branch
        assertEquals(79.99, page.get("price"));                  // from product branch
        assertNotNull(page.get("available"));                    // from inventory branch
        assertNotNull(page.get("stock"));                        // from inventory branch
        assertNotNull(page.get("rating"));                       // from reviews branch
        assertNotNull(page.get("reviewCount"));                  // from reviews branch
    }

    @Test
    void branchesAreIndependentNoSharedState() {
        // Run branches for two different products and verify no cross-contamination
        GetProductWorker productWorker = new GetProductWorker();
        GetInventoryWorker inventoryWorker = new GetInventoryWorker();
        GetReviewsWorker reviewsWorker = new GetReviewsWorker();

        // Product A
        TaskResult prodA = productWorker.execute(taskWith(Map.of("productId", "PROD-001")));
        TaskResult invA = inventoryWorker.execute(taskWith(Map.of("productId", "PROD-001")));
        TaskResult revA = reviewsWorker.execute(taskWith(Map.of("productId", "PROD-001")));

        // Product B
        TaskResult prodB = productWorker.execute(taskWith(Map.of("productId", "PROD-002")));
        TaskResult invB = inventoryWorker.execute(taskWith(Map.of("productId", "PROD-002")));
        TaskResult revB = reviewsWorker.execute(taskWith(Map.of("productId", "PROD-002")));

        // Verify independence: product names differ
        @SuppressWarnings("unchecked")
        Map<String, Object> productA = (Map<String, Object>) prodA.getOutputData().get("product");
        @SuppressWarnings("unchecked")
        Map<String, Object> productB = (Map<String, Object>) prodB.getOutputData().get("product");

        assertNotEquals(productA.get("name"), productB.get("name"),
                "Different products should have different names — no shared state");
        assertNotEquals(productA.get("id"), productB.get("id"),
                "Different products should have different IDs");

        // Verify inventory independence
        @SuppressWarnings("unchecked")
        Map<String, Object> invAData = (Map<String, Object>) invA.getOutputData().get("inventory");
        @SuppressWarnings("unchecked")
        Map<String, Object> invBData = (Map<String, Object>) invB.getOutputData().get("inventory");
        assertNotEquals(invAData.get("productId"), invBData.get("productId"));
    }

    @Test
    void oneBranchFailingCausesImmediateFailureInMerge() {
        // Simulate: product branch fails (unknown ID), but inventory and reviews succeed
        GetProductWorker productWorker = new GetProductWorker();
        TaskResult productResult = productWorker.execute(taskWith(Map.of("productId", "PROD-999")));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, productResult.getStatus(),
                "Product branch should fail for unknown product");

        // Inventory and reviews would succeed for any valid product
        GetInventoryWorker inventoryWorker = new GetInventoryWorker();
        GetReviewsWorker reviewsWorker = new GetReviewsWorker();
        TaskResult invResult = inventoryWorker.execute(taskWith(Map.of("productId", "PROD-001")));
        TaskResult revResult = reviewsWorker.execute(taskWith(Map.of("productId", "PROD-001")));
        assertEquals(TaskResult.Status.COMPLETED, invResult.getStatus());
        assertEquals(TaskResult.Status.COMPLETED, revResult.getStatus());

        // Merge should fail because product is null (simulating failed branch)
        MergeResultsWorker mergeWorker = new MergeResultsWorker();
        Map<String, Object> mergeInput = new HashMap<>();
        mergeInput.put("product", null); // product branch failed
        mergeInput.put("inventory", invResult.getOutputData().get("inventory"));
        mergeInput.put("reviews", revResult.getOutputData().get("reviews"));

        TaskResult mergeResult = mergeWorker.execute(taskWith(mergeInput));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, mergeResult.getStatus(),
                "Merge must fail when any fork branch is missing");
    }

    @Test
    void mergeOutputContainsResultsFromAllBranches() {
        // Verify the merge contains exactly the 6 fields sourced from 3 branches
        Map<String, Object> product = Map.of("name", "Test Product", "price", 29.99);
        Map<String, Object> inventory = Map.of("inStock", true, "quantity", 50);
        Map<String, Object> reviews = Map.of("averageRating", 4.2, "totalReviews", 100);

        MergeResultsWorker mergeWorker = new MergeResultsWorker();
        TaskResult result = mergeWorker.execute(
                taskWith(Map.of("product", product, "inventory", inventory, "reviews", reviews)));

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> page = (Map<String, Object>) result.getOutputData().get("productPage");

        // Fields from product branch
        assertEquals("Test Product", page.get("name"));
        assertEquals(29.99, page.get("price"));

        // Fields from inventory branch
        assertEquals(true, page.get("available"));
        assertEquals(50, page.get("stock"));

        // Fields from reviews branch
        assertEquals(4.2, page.get("rating"));
        assertEquals(100, page.get("reviewCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
