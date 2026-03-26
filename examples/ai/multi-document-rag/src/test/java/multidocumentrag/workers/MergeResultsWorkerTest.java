package multidocumentrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergeResultsWorkerTest {

    private final MergeResultsWorker worker = new MergeResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("mdrag_merge_results", worker.getTaskDefName());
    }

    @Test
    void mergesAndSortsByScoreDescending() {
        List<Map<String, Object>> apiDocs = new ArrayList<>(List.of(
                new HashMap<>(Map.of("text", "api1", "source", "api_docs", "score", 0.95)),
                new HashMap<>(Map.of("text", "api2", "source", "api_docs", "score", 0.88))
        ));
        List<Map<String, Object>> tutorials = new ArrayList<>(List.of(
                new HashMap<>(Map.of("text", "tut1", "source", "tutorials", "score", 0.92)),
                new HashMap<>(Map.of("text", "tut2", "source", "tutorials", "score", 0.85))
        ));
        List<Map<String, Object>> forums = new ArrayList<>(List.of(
                new HashMap<>(Map.of("text", "forum1", "source", "forums", "score", 0.90))
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "apiDocs", apiDocs,
                "tutorials", tutorials,
                "forums", forums)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> merged = (List<Map<String, Object>>) result.getOutputData().get("merged");
        assertNotNull(merged);
        assertEquals(5, merged.size());

        // Verify sorted descending by score
        double prev = Double.MAX_VALUE;
        for (Map<String, Object> m : merged) {
            double score = ((Number) m.get("score")).doubleValue();
            assertTrue(score <= prev, "Results should be sorted by score descending");
            prev = score;
        }

        assertEquals(5, result.getOutputData().get("totalResults"));

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceCounts = (Map<String, Object>) result.getOutputData().get("sourceCounts");
        assertEquals(2, sourceCounts.get("api_docs"));
        assertEquals(2, sourceCounts.get("tutorials"));
        assertEquals(1, sourceCounts.get("forums"));
    }

    @Test
    void handlesEmptyInputs() {
        Task task = taskWith(new HashMap<>(Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalResults"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
