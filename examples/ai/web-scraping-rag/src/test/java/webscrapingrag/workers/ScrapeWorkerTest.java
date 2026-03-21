package webscrapingrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScrapeWorkerTest {

    private final ScrapeWorker worker = new ScrapeWorker();

    @Test
    void taskDefName() {
        assertEquals("wsrag_scrape", worker.getTaskDefName());
    }

    @Test
    void scrapesUrlsAndReturnsPages() {
        Task task = taskWith(new HashMap<>(Map.of(
                "urls", List.of("https://docs.example.com/overview", "https://docs.example.com/workers")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pages = (List<Map<String, Object>>) result.getOutputData().get("pages");
        assertNotNull(pages);
        assertEquals(2, pages.size());
        assertEquals(2, result.getOutputData().get("pageCount"));
    }

    @Test
    void firstPageHasCorrectFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "urls", List.of("https://docs.example.com/overview", "https://docs.example.com/workers")
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pages = (List<Map<String, Object>>) result.getOutputData().get("pages");
        Map<String, Object> first = pages.get(0);
        assertEquals("https://docs.example.com/overview", first.get("url"));
        assertEquals("Conductor Docs - Overview", first.get("title"));
        assertEquals(24, first.get("wordCount"));
        assertTrue(first.get("text").toString().contains("Orkes Conductor"));
    }

    @Test
    void secondPageHasCorrectFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "urls", List.of("https://docs.example.com/overview", "https://docs.example.com/workers")
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pages = (List<Map<String, Object>>) result.getOutputData().get("pages");
        Map<String, Object> second = pages.get(1);
        assertEquals("https://docs.example.com/workers", second.get("url"));
        assertEquals("Conductor Docs - Workers", second.get("title"));
        assertEquals(27, second.get("wordCount"));
        assertTrue(second.get("text").toString().contains("building blocks"));
    }

    @Test
    void outputContainsPageCount() {
        Task task = taskWith(new HashMap<>(Map.of(
                "urls", List.of("https://docs.example.com/overview", "https://docs.example.com/workers")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("pageCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
