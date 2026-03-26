package webbrowsing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReadPageWorkerTest {

    private final ReadPageWorker worker = new ReadPageWorker();

    @Test
    void taskDefName() {
        assertEquals("wb_read_page", worker.getTaskDefName());
    }

    @Test
    void readsAllSelectedPages() {
        List<Map<String, Object>> pages = List.of(
                Map.of("url", "https://a.com", "title", "Page A"),
                Map.of("url", "https://b.com", "title", "Page B"),
                Map.of("url", "https://c.com", "title", "Page C")
        );

        Task task = taskWith(Map.of("selectedPages", pages));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents =
                (List<Map<String, Object>>) result.getOutputData().get("pageContents");
        assertNotNull(contents);
        assertEquals(3, contents.size());
    }

    @Test
    void eachPageContentHasRequiredFields() {
        List<Map<String, Object>> pages = List.of(
                Map.of("url", "https://a.com", "title", "Page A")
        );

        Task task = taskWith(Map.of("selectedPages", pages));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents =
                (List<Map<String, Object>>) result.getOutputData().get("pageContents");
        Map<String, Object> page = contents.get(0);
        assertNotNull(page.get("url"));
        assertNotNull(page.get("title"));
        assertNotNull(page.get("content"));
        assertNotNull(page.get("wordCount"));
        assertNotNull(page.get("loadTimeMs"));
    }

    @Test
    void wordCountIsPositive() {
        List<Map<String, Object>> pages = List.of(
                Map.of("url", "https://a.com", "title", "Page A")
        );

        Task task = taskWith(Map.of("selectedPages", pages));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents =
                (List<Map<String, Object>>) result.getOutputData().get("pageContents");
        int wordCount = ((Number) contents.get(0).get("wordCount")).intValue();
        assertTrue(wordCount > 0, "Word count should be positive");
    }

    @Test
    void totalWordsReadSumsPageCounts() {
        List<Map<String, Object>> pages = List.of(
                Map.of("url", "https://a.com", "title", "Page A"),
                Map.of("url", "https://b.com", "title", "Page B")
        );

        Task task = taskWith(Map.of("selectedPages", pages));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents =
                (List<Map<String, Object>>) result.getOutputData().get("pageContents");
        int expectedTotal = 0;
        for (Map<String, Object> c : contents) {
            expectedTotal += ((Number) c.get("wordCount")).intValue();
        }
        assertEquals(expectedTotal, result.getOutputData().get("totalWordsRead"));
    }

    @Test
    void loadTimesAreDeterministic() {
        List<Map<String, Object>> pages = List.of(
                Map.of("url", "https://a.com", "title", "A"),
                Map.of("url", "https://b.com", "title", "B")
        );

        Task task = taskWith(Map.of("selectedPages", pages));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents =
                (List<Map<String, Object>>) result.getOutputData().get("pageContents");
        assertEquals(120, ((Number) contents.get(0).get("loadTimeMs")).intValue());
        assertEquals(165, ((Number) contents.get(1).get("loadTimeMs")).intValue());
    }

    @Test
    void handlesEmptySelectedPages() {
        Task task = taskWith(Map.of("selectedPages", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents =
                (List<Map<String, Object>>) result.getOutputData().get("pageContents");
        assertEquals(0, contents.size());
        assertEquals(0, result.getOutputData().get("totalWordsRead"));
    }

    @Test
    void handlesNullSelectedPages() {
        Map<String, Object> input = new HashMap<>();
        input.put("selectedPages", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents =
                (List<Map<String, Object>>) result.getOutputData().get("pageContents");
        assertEquals(0, contents.size());
    }

    @Test
    void preservesOriginalUrlAndTitle() {
        List<Map<String, Object>> pages = List.of(
                Map.of("url", "https://conductor.example.com", "title", "Conductor Docs")
        );

        Task task = taskWith(Map.of("selectedPages", pages));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents =
                (List<Map<String, Object>>) result.getOutputData().get("pageContents");
        assertEquals("https://conductor.example.com", contents.get(0).get("url"));
        assertEquals("Conductor Docs", contents.get(0).get("title"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
