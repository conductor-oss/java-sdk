package recommendationengine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComputeSimilarityWorkerTest {

    private final ComputeSimilarityWorker worker = new ComputeSimilarityWorker();

    @Test
    void taskDefName() {
        assertEquals("rec_compute_similarity", worker.getTaskDefName());
    }

    @Test
    void returnsSimilarProducts() {
        Task task = taskWith(Map.of("userId", "U-1",
                "viewedProducts", List.of("SKU-101"),
                "purchasedProducts", List.of("SKU-050")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) result.getOutputData().get("similarProducts");
        assertNotNull(products);
        assertEquals(5, products.size());
    }

    @Test
    void productsHaveSkuScoreAndCategory() {
        Task task = taskWith(Map.of("userId", "U-2"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) result.getOutputData().get("similarProducts");
        Map<String, Object> first = products.get(0);
        assertTrue(first.containsKey("sku"));
        assertTrue(first.containsKey("score"));
        assertTrue(first.containsKey("category"));
    }

    @Test
    void firstProductHasHighestScore() {
        Task task = taskWith(Map.of("userId", "U-3"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) result.getOutputData().get("similarProducts");
        double firstScore = ((Number) products.get(0).get("score")).doubleValue();
        assertEquals(0.95, firstScore, 0.001);
    }

    @Test
    void lastProductHasLowestScore() {
        Task task = taskWith(Map.of("userId", "U-4"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) result.getOutputData().get("similarProducts");
        double lastScore = ((Number) products.get(products.size() - 1).get("score")).doubleValue();
        assertEquals(0.71, lastScore, 0.001);
    }

    @Test
    void containsAudioCategory() {
        Task task = taskWith(Map.of("userId", "U-5"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) result.getOutputData().get("similarProducts");
        boolean hasAudio = products.stream().anyMatch(p -> "audio".equals(p.get("category")));
        assertTrue(hasAudio);
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("similarProducts"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
