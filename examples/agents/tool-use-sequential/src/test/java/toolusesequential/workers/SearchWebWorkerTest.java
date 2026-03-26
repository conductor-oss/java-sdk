package toolusesequential.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchWebWorkerTest {

    private final SearchWebWorker worker = new SearchWebWorker();

    @Test
    void taskDefName() {
        assertEquals("ts_search_web", worker.getTaskDefName());
    }

    @Test
    void returnsThreeSearchResults() {
        Task task = taskWith(Map.of("query", "Conductor workflow orchestration", "maxResults", 5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    void eachResultHasUrlTitleSnippet() {
        Task task = taskWith(Map.of("query", "Conductor"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) result.getOutputData().get("results");
        for (Map<String, String> r : results) {
            assertNotNull(r.get("url"));
            assertNotNull(r.get("title"));
            assertNotNull(r.get("snippet"));
        }
    }

    @Test
    void topResultMatchesFirstResult() {
        Task task = taskWith(Map.of("query", "Conductor"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) result.getOutputData().get("results");
        @SuppressWarnings("unchecked")
        Map<String, String> topResult = (Map<String, String>) result.getOutputData().get("topResult");

        assertNotNull(topResult);
        assertEquals(results.get(0).get("url"), topResult.get("url"));
        assertEquals(results.get(0).get("title"), topResult.get("title"));
    }

    @Test
    void returnsTotalFound() {
        Task task = taskWith(Map.of("query", "Conductor"));
        TaskResult result = worker.execute(task);

        assertEquals(2450, result.getOutputData().get("totalFound"));
    }

    @Test
    void handlesNullQuery() {
        Map<String, Object> input = new HashMap<>();
        input.put("query", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("results"));
    }

    @Test
    void handlesMissingQuery() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("results"));
    }

    @Test
    void handlesNonNumericMaxResults() {
        Task task = taskWith(Map.of("query", "test", "maxResults", "abc"));
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
