package tooluseconditional.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebSearchWorkerTest {

    private final WebSearchWorker worker = new WebSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("tc_web_search", worker.getTaskDefName());
    }

    @Test
    void returnsTwoSearchResults() {
        Task task = taskWith(Map.of("searchQuery", "capital of France", "userQuery", "What is the capital of France?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void resultsTitlesContainQuery() {
        Task task = taskWith(Map.of("searchQuery", "quantum computing"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        String title = (String) results.get(0).get("title");
        assertTrue(title.contains("quantum computing"));
    }

    @Test
    void returnsTotalFound() {
        Task task = taskWith(Map.of("searchQuery", "machine learning"));
        TaskResult result = worker.execute(task);

        assertEquals(1250, result.getOutputData().get("totalFound"));
    }

    @Test
    void returnsToolUsedWebSearch() {
        Task task = taskWith(Map.of("searchQuery", "test query"));
        TaskResult result = worker.execute(task);

        assertEquals("web_search", result.getOutputData().get("toolUsed"));
    }

    @Test
    void answerContainsSearchQuery() {
        Task task = taskWith(Map.of("searchQuery", "climate change effects"));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("climate change effects"));
    }

    @Test
    void resultsContainUrls() {
        Task task = taskWith(Map.of("searchQuery", "best practices"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        String url = (String) results.get(0).get("url");
        assertTrue(url.startsWith("https://example.com/"));
        assertTrue(url.contains("best-practices"));
    }

    @Test
    void handlesNullSearchQuery() {
        Map<String, Object> input = new HashMap<>();
        input.put("searchQuery", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("results"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
