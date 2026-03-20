package webbrowsing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SelectPagesWorkerTest {

    private final SelectPagesWorker worker = new SelectPagesWorker();

    @Test
    void taskDefName() {
        assertEquals("wb_select_pages", worker.getTaskDefName());
    }

    @Test
    void selectsTopNByRelevance() {
        List<Map<String, Object>> searchResults = List.of(
                Map.of("url", "https://a.com", "title", "A", "relevance", 0.70),
                Map.of("url", "https://b.com", "title", "B", "relevance", 0.95),
                Map.of("url", "https://c.com", "title", "C", "relevance", 0.80),
                Map.of("url", "https://d.com", "title", "D", "relevance", 0.90)
        );

        Task task = taskWith(Map.of(
                "searchResults", searchResults,
                "question", "test question",
                "maxPages", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selected =
                (List<Map<String, Object>>) result.getOutputData().get("selectedPages");
        assertEquals(2, selected.size());
        assertEquals("https://b.com", selected.get(0).get("url"));
        assertEquals("https://d.com", selected.get(1).get("url"));
    }

    @Test
    void returnsPageCount() {
        List<Map<String, Object>> searchResults = List.of(
                Map.of("url", "https://a.com", "title", "A", "relevance", 0.90),
                Map.of("url", "https://b.com", "title", "B", "relevance", 0.80)
        );

        Task task = taskWith(Map.of(
                "searchResults", searchResults,
                "question", "test",
                "maxPages", 3));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("pageCount"));
    }

    @Test
    void defaultsToThreeMaxPages() {
        List<Map<String, Object>> searchResults = List.of(
                Map.of("url", "https://a.com", "title", "A", "relevance", 0.95),
                Map.of("url", "https://b.com", "title", "B", "relevance", 0.90),
                Map.of("url", "https://c.com", "title", "C", "relevance", 0.85),
                Map.of("url", "https://d.com", "title", "D", "relevance", 0.80),
                Map.of("url", "https://e.com", "title", "E", "relevance", 0.75)
        );

        Task task = taskWith(Map.of(
                "searchResults", searchResults,
                "question", "test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selected =
                (List<Map<String, Object>>) result.getOutputData().get("selectedPages");
        assertEquals(3, selected.size());
    }

    @Test
    void handlesEmptySearchResults() {
        Task task = taskWith(Map.of(
                "searchResults", List.of(),
                "question", "test",
                "maxPages", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selected =
                (List<Map<String, Object>>) result.getOutputData().get("selectedPages");
        assertEquals(0, selected.size());
        assertEquals(0, result.getOutputData().get("pageCount"));
    }

    @Test
    void handlesNullSearchResults() {
        Map<String, Object> input = new HashMap<>();
        input.put("searchResults", null);
        input.put("question", "test");
        input.put("maxPages", 2);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("pageCount"));
    }

    @Test
    void handlesNullQuestion() {
        List<Map<String, Object>> searchResults = List.of(
                Map.of("url", "https://a.com", "title", "A", "relevance", 0.90)
        );

        Map<String, Object> input = new HashMap<>();
        input.put("searchResults", searchResults);
        input.put("question", null);
        input.put("maxPages", 2);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void sortOrderIsDescending() {
        List<Map<String, Object>> searchResults = List.of(
                Map.of("url", "https://low.com", "title", "Low", "relevance", 0.50),
                Map.of("url", "https://high.com", "title", "High", "relevance", 0.99),
                Map.of("url", "https://mid.com", "title", "Mid", "relevance", 0.75)
        );

        Task task = taskWith(Map.of(
                "searchResults", searchResults,
                "question", "test",
                "maxPages", 3));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selected =
                (List<Map<String, Object>>) result.getOutputData().get("selectedPages");
        assertEquals("https://high.com", selected.get(0).get("url"));
        assertEquals("https://mid.com", selected.get(1).get("url"));
        assertEquals("https://low.com", selected.get(2).get("url"));
    }

    @Test
    void maxPagesLimitsOutput() {
        List<Map<String, Object>> searchResults = List.of(
                Map.of("url", "https://a.com", "title", "A", "relevance", 0.95),
                Map.of("url", "https://b.com", "title", "B", "relevance", 0.90),
                Map.of("url", "https://c.com", "title", "C", "relevance", 0.85)
        );

        Task task = taskWith(Map.of(
                "searchResults", searchResults,
                "question", "test",
                "maxPages", 1));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selected =
                (List<Map<String, Object>>) result.getOutputData().get("selectedPages");
        assertEquals(1, selected.size());
        assertEquals(1, result.getOutputData().get("pageCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
