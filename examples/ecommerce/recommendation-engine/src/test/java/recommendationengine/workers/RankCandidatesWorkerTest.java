package recommendationengine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RankCandidatesWorkerTest {

    private final RankCandidatesWorker worker = new RankCandidatesWorker();

    @Test
    void taskDefName() {
        assertEquals("rec_rank_candidates", worker.getTaskDefName());
    }

    @Test
    void ranksProductsByScoreDescending() {
        List<Map<String, Object>> products = List.of(
                Map.of("sku", "A", "score", 0.5, "category", "x"),
                Map.of("sku", "B", "score", 0.9, "category", "y"),
                Map.of("sku", "C", "score", 0.7, "category", "z"));
        Task task = taskWith(Map.of("similarProducts", products));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ranked = (List<Map<String, Object>>) result.getOutputData().get("rankedProducts");
        assertEquals("B", ranked.get(0).get("sku"));
        assertEquals("C", ranked.get(1).get("sku"));
        assertEquals("A", ranked.get(2).get("sku"));
    }

    @Test
    void assignsRankNumbers() {
        List<Map<String, Object>> products = List.of(
                Map.of("sku", "A", "score", 0.8, "category", "x"),
                Map.of("sku", "B", "score", 0.6, "category", "y"));
        Task task = taskWith(Map.of("similarProducts", products));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ranked = (List<Map<String, Object>>) result.getOutputData().get("rankedProducts");
        assertEquals(1, ranked.get(0).get("rank"));
        assertEquals(2, ranked.get(1).get("rank"));
    }

    @Test
    void outputsTotalCandidates() {
        List<Map<String, Object>> products = List.of(
                Map.of("sku", "A", "score", 0.5, "category", "x"),
                Map.of("sku", "B", "score", 0.3, "category", "y"),
                Map.of("sku", "C", "score", 0.9, "category", "z"));
        Task task = taskWith(Map.of("similarProducts", products));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("totalCandidates"));
    }

    @Test
    void handlesEmptyList() {
        Task task = taskWith(Map.of("similarProducts", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalCandidates"));
    }

    @Test
    void handlesNullSimilarProducts() {
        Map<String, Object> input = new HashMap<>();
        input.put("similarProducts", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalCandidates"));
    }

    @Test
    void handlesSingleProduct() {
        List<Map<String, Object>> products = List.of(
                Map.of("sku", "ONLY", "score", 0.99, "category", "solo"));
        Task task = taskWith(Map.of("similarProducts", products));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ranked = (List<Map<String, Object>>) result.getOutputData().get("rankedProducts");
        assertEquals(1, ranked.size());
        assertEquals(1, ranked.get(0).get("rank"));
        assertEquals(1, result.getOutputData().get("totalCandidates"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
