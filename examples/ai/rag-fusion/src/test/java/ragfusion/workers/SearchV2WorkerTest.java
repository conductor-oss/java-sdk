package ragfusion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchV2WorkerTest {

    private final SearchV2Worker worker = new SearchV2Worker();

    @Test
    void taskDefName() {
        assertEquals("rf_search_v2", worker.getTaskDefName());
    }

    @Test
    void returnsFourResults() {
        Task task = taskWith(new HashMap<>(Map.of("query", "test query", "variantIndex", 2)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(4, results.size());
    }

    @Test
    void resultsHaveIdTextAndRank() {
        Task task = taskWith(new HashMap<>(Map.of("query", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        for (Map<String, Object> doc : results) {
            assertNotNull(doc.get("id"));
            assertNotNull(doc.get("text"));
            assertNotNull(doc.get("rank"));
            assertInstanceOf(String.class, doc.get("id"));
            assertInstanceOf(String.class, doc.get("text"));
            assertInstanceOf(Integer.class, doc.get("rank"));
        }
    }

    @Test
    void ranksAreSequential() {
        Task task = taskWith(new HashMap<>(Map.of("query", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        for (int i = 0; i < results.size(); i++) {
            assertEquals(i + 1, results.get(i).get("rank"));
        }
    }

    @Test
    void returnsSourceIdentifier() {
        Task task = taskWith(new HashMap<>(Map.of("query", "test")));
        TaskResult result = worker.execute(task);

        assertEquals("search_v2", result.getOutputData().get("source"));
    }

    @Test
    void idsHaveV2Prefix() {
        Task task = taskWith(new HashMap<>(Map.of("query", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        for (Map<String, Object> doc : results) {
            assertTrue(((String) doc.get("id")).startsWith("v2-"));
        }
    }

    @Test
    void handlesMissingQuery() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("results"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
