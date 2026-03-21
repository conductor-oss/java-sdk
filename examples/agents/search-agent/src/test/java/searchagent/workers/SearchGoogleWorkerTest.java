package searchagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchGoogleWorkerTest {

    private final SearchGoogleWorker worker = new SearchGoogleWorker();

    @Test
    void taskDefName() {
        assertEquals("sa_search_google", worker.getTaskDefName());
    }

    @Test
    void returnsResultsWithExpectedFields() {
        Task task = taskWith(Map.of(
                "queries", List.of("quantum computing 2026"),
                "maxResults", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(3, results.size());

        Map<String, Object> first = results.get(0);
        assertNotNull(first.get("title"));
        assertNotNull(first.get("url"));
        assertNotNull(first.get("snippet"));
        assertNotNull(first.get("relevance"));
        assertEquals("google", first.get("source"));
    }

    @Test
    void respectsMaxResults() {
        Task task = taskWith(Map.of(
                "queries", List.of("test query"),
                "maxResults", 2));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(2, results.size());
    }

    @Test
    void returnsTotalScannedCount() {
        Task task = taskWith(Map.of(
                "queries", List.of("test"),
                "maxResults", 5));
        TaskResult result = worker.execute(task);

        assertEquals(342, result.getOutputData().get("totalScanned"));
    }

    @Test
    void returnsSearchEngineGoogle() {
        Task task = taskWith(Map.of(
                "queries", List.of("test"),
                "maxResults", 5));
        TaskResult result = worker.execute(task);

        assertEquals("google", result.getOutputData().get("searchEngine"));
    }

    @Test
    void handlesNullQueries() {
        Map<String, Object> input = new HashMap<>();
        input.put("queries", null);
        input.put("maxResults", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("results"));
    }

    @Test
    void defaultsMaxResultsToFive() {
        Task task = taskWith(Map.of("queries", List.of("test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(5, results.size());
    }

    @Test
    void allResultsHaveGoogleSource() {
        Task task = taskWith(Map.of(
                "queries", List.of("test"),
                "maxResults", 5));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        for (Map<String, Object> r : results) {
            assertEquals("google", r.get("source"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
