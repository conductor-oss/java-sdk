package raghybridsearch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VectorSearchWorkerTest {

    private final VectorSearchWorker worker = new VectorSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("hs_vector_search", worker.getTaskDefName());
    }

    @Test
    void returnsResultsForRelevantQuery() {
        Task task = taskWith(Map.of("question", "Conductor workflow orchestration"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Cosine similarity should find relevant documents");
        assertTrue(results.size() <= 3, "Should return at most 3 results");
    }

    @Test
    void scoresAreNormalized() {
        Task task = taskWith(Map.of("question", "Conductor server engine"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertFalse(results.isEmpty());

        for (Map<String, Object> r : results) {
            double score = ((Number) r.get("score")).doubleValue();
            assertTrue(score >= 0.0 && score <= 1.0, "Cosine similarity should be in [0, 1]");
        }
    }

    @Test
    void resultsAreSortedByScoreDescending() {
        Task task = taskWith(Map.of("question", "workflow task JSON"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");

        for (int i = 1; i < results.size(); i++) {
            double prev = ((Number) results.get(i - 1).get("score")).doubleValue();
            double curr = ((Number) results.get(i).get("score")).doubleValue();
            assertTrue(prev >= curr, "Results should be sorted by score descending");
        }
    }

    @Test
    void resultsContainIdScoreAndText() {
        Task task = taskWith(Map.of("question", "workers poll tasks"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertFalse(results.isEmpty());

        Map<String, Object> first = results.get(0);
        assertNotNull(first.get("id"));
        assertNotNull(first.get("score"));
        assertNotNull(first.get("text"));
    }

    @Test
    void countMatchesResultsSize() {
        Task task = taskWith(Map.of("question", "retry timeout"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(results.size(), result.getOutputData().get("count"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
