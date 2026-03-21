package webbrowsing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteSearchWorkerTest {

    private final ExecuteSearchWorker worker = new ExecuteSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("wb_execute_search", worker.getTaskDefName());
    }

    @Test
    void returnsFiveResults() {
        Task task = taskWith(Map.of(
                "searchQueries", List.of("Conductor features", "workflow engine"),
                "searchEngine", "google"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(5, results.size());
    }

    @Test
    void resultsTotalMatchesListSize() {
        Task task = taskWith(Map.of(
                "searchQueries", List.of("test query"),
                "searchEngine", "google"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(results.size(), result.getOutputData().get("totalResults"));
    }

    @Test
    void eachResultHasRequiredFields() {
        Task task = taskWith(Map.of(
                "searchQueries", List.of("Conductor"),
                "searchEngine", "google"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        for (Map<String, Object> r : results) {
            assertNotNull(r.get("url"));
            assertNotNull(r.get("title"));
            assertNotNull(r.get("snippet"));
            assertNotNull(r.get("relevance"));
        }
    }

    @Test
    void relevanceScoresAreBetweenZeroAndOne() {
        Task task = taskWith(Map.of(
                "searchQueries", List.of("Conductor workflow"),
                "searchEngine", "google"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        for (Map<String, Object> r : results) {
            double relevance = ((Number) r.get("relevance")).doubleValue();
            assertTrue(relevance > 0 && relevance <= 1.0,
                    "Relevance score should be between 0 and 1: " + relevance);
        }
    }

    @Test
    void handlesNullSearchQueries() {
        Map<String, Object> input = new HashMap<>();
        input.put("searchQueries", null);
        input.put("searchEngine", "google");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("results"));
    }

    @Test
    void handlesMissingSearchEngine() {
        Task task = taskWith(Map.of("searchQueries", List.of("test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("results"));
    }

    @Test
    void handlesEmptySearchQueries() {
        Task task = taskWith(Map.of("searchQueries", List.of(), "searchEngine", "google"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(5, results.size());
    }

    @Test
    void urlsAreValidFormat() {
        Task task = taskWith(Map.of(
                "searchQueries", List.of("Conductor"),
                "searchEngine", "google"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        for (Map<String, Object> r : results) {
            String url = (String) r.get("url");
            assertTrue(url.startsWith("https://"), "URL should start with https://: " + url);
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
