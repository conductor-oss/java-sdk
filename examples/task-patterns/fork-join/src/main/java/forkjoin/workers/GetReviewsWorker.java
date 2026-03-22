package forkjoin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fetches review data for a given product ID.
 * Computes deterministic review metrics based on the product ID hash,
 * representing reviews API lookup results.
 */
public class GetReviewsWorker implements Worker {

    private static final String[] SAMPLE_REVIEWS = {
            "Great sound quality and comfortable fit!",
            "Excellent build quality, highly recommended.",
            "Good value for the price.",
            "Works as expected, no complaints.",
            "Solid product, would buy again."
    };

    @Override
    public String getTaskDefName() {
        return "fj_get_reviews";
    }

    @Override
    public TaskResult execute(Task task) {
        String productId = (String) task.getInputData().get("productId");
        if (productId == null || productId.isBlank()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'productId' is required and must not be blank");
            return fail;
        }

        System.out.println("  [fj_get_reviews] Fetching reviews for: " + productId);

        // Deterministic review data based on product ID
        int hash = Math.abs(productId.hashCode());
        double averageRating = 1.0 + (hash % 41) / 10.0; // range 1.0 - 5.0
        int totalReviews = hash % 1000;
        String topReview = SAMPLE_REVIEWS[hash % SAMPLE_REVIEWS.length];

        Map<String, Object> reviews = new LinkedHashMap<>();
        reviews.put("productId", productId);
        reviews.put("averageRating", averageRating);
        reviews.put("totalReviews", totalReviews);
        reviews.put("topReview", topReview);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reviews", reviews);
        return result;
    }
}
