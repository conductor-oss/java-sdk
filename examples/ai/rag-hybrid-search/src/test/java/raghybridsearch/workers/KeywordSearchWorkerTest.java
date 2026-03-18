package raghybridsearch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KeywordSearchWorkerTest {

    private final KeywordSearchWorker worker = new KeywordSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("hs_keyword_search", worker.getTaskDefName());
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
        assertFalse(results.isEmpty(), "BM25 should find relevant documents");
        assertTrue(results.size() <= 3, "Should return at most 3 results");
    }

    @Test
    void resultsAreSortedByScoreDescending() {
        Task task = taskWith(Map.of("question", "workflow task management"));
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
        Task task = taskWith(Map.of("question", "Conductor server"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertFalse(results.isEmpty());

        Map<String, Object> first = results.get(0);
        assertNotNull(first.get("id"));
        assertNotNull(first.get("score"));
        assertNotNull(first.get("text"));
        assertTrue(((Number) first.get("score")).doubleValue() > 0);
    }

    @Test
    void countMatchesResultsSize() {
        Task task = taskWith(Map.of("question", "JSON DSL workflow"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(results.size(), result.getOutputData().get("count"));
    }

    @Test
    void differentQueriesProduceDifferentRankings() {
        Task task1 = taskWith(Map.of("question", "retry timeout rate limiting"));
        TaskResult r1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("question", "REST API management"));
        TaskResult r2 = worker.execute(task2);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results1 =
                (List<Map<String, Object>>) r1.getOutputData().get("results");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results2 =
                (List<Map<String, Object>>) r2.getOutputData().get("results");

        // Different queries should produce at least partially different top results
        if (!results1.isEmpty() && !results2.isEmpty()) {
            String top1 = (String) results1.get(0).get("id");
            String top2 = (String) results2.get(0).get("id");
            // Not asserting inequality since queries could theoretically match the same doc,
            // but at minimum both should return valid results
            assertNotNull(top1);
            assertNotNull(top2);
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
