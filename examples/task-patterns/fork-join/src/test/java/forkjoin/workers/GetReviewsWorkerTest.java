package forkjoin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GetReviewsWorkerTest {

    private final GetReviewsWorker worker = new GetReviewsWorker();

    @Test
    void taskDefName() {
        assertEquals("fj_get_reviews", worker.getTaskDefName());
    }

    @Test
    void returnsReviewData() {
        Task task = taskWith(Map.of("productId", "PROD-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> reviews = (Map<String, Object>) result.getOutputData().get("reviews");
        assertNotNull(reviews);
        assertEquals("PROD-001", reviews.get("productId"));
        assertNotNull(reviews.get("averageRating"));
        assertNotNull(reviews.get("totalReviews"));
        assertNotNull(reviews.get("topReview"));
    }

    @Test
    void ratingIsInValidRange() {
        Task task = taskWith(Map.of("productId", "PROD-001"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> reviews = (Map<String, Object>) result.getOutputData().get("reviews");
        double rating = ((Number) reviews.get("averageRating")).doubleValue();
        assertTrue(rating >= 1.0 && rating <= 5.0,
                "Rating should be between 1.0 and 5.0, got " + rating);
    }

    @Test
    void reviewDataIsDeterministic() {
        Task task1 = taskWith(Map.of("productId", "PROD-001"));
        Task task2 = taskWith(Map.of("productId", "PROD-001"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        @SuppressWarnings("unchecked")
        Map<String, Object> reviews1 = (Map<String, Object>) result1.getOutputData().get("reviews");
        @SuppressWarnings("unchecked")
        Map<String, Object> reviews2 = (Map<String, Object>) result2.getOutputData().get("reviews");

        assertEquals(reviews1.get("averageRating"), reviews2.get("averageRating"));
        assertEquals(reviews1.get("totalReviews"), reviews2.get("totalReviews"));
        assertEquals(reviews1.get("topReview"), reviews2.get("topReview"));
    }

    @Test
    void differentProductsGetDifferentReviews() {
        Task task1 = taskWith(Map.of("productId", "PROD-001"));
        Task task2 = taskWith(Map.of("productId", "PROD-002"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        @SuppressWarnings("unchecked")
        Map<String, Object> reviews1 = (Map<String, Object>) result1.getOutputData().get("reviews");
        @SuppressWarnings("unchecked")
        Map<String, Object> reviews2 = (Map<String, Object>) result2.getOutputData().get("reviews");

        assertEquals("PROD-001", reviews1.get("productId"));
        assertEquals("PROD-002", reviews2.get("productId"));
    }

    @Test
    void failsOnMissingProductId() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("productId"));
    }

    @Test
    void failsOnBlankProductId() {
        Task task = taskWith(Map.of("productId", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("productId"));
    }

    @Test
    void failsOnNullProductId() {
        Map<String, Object> input = new HashMap<>();
        input.put("productId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void reviewsContainAllExpectedFields() {
        Task task = taskWith(Map.of("productId", "PROD-001"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> reviews = (Map<String, Object>) result.getOutputData().get("reviews");
        assertEquals(4, reviews.size());
        assertTrue(reviews.containsKey("productId"));
        assertTrue(reviews.containsKey("averageRating"));
        assertTrue(reviews.containsKey("totalReviews"));
        assertTrue(reviews.containsKey("topReview"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
