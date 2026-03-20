package toolusesequential.workers;

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
        assertEquals("ts_read_page", worker.getTaskDefName());
    }

    @Test
    void returnsContentWithFourSections() {
        Task task = taskWith(Map.of("url", "https://orkes.io/docs", "title", "Orkes Docs"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) result.getOutputData().get("content");
        assertNotNull(content);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) content.get("sections");
        assertNotNull(sections);
        assertEquals(4, sections.size());
    }

    @Test
    void contentIncludesProvidedTitleAndUrl() {
        Task task = taskWith(Map.of("url", "https://example.com/page", "title", "My Page Title"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) result.getOutputData().get("content");
        assertEquals("My Page Title", content.get("title"));
        assertEquals("https://example.com/page", content.get("url"));
    }

    @Test
    void contentIncludesWordCount() {
        Task task = taskWith(Map.of("url", "https://example.com", "title", "Test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) result.getOutputData().get("content");
        assertEquals(1850, content.get("wordCount"));
    }

    @Test
    void returnsStatusCode200() {
        Task task = taskWith(Map.of("url", "https://example.com"));
        TaskResult result = worker.execute(task);

        assertEquals(200, result.getOutputData().get("statusCode"));
    }

    @Test
    void returnsLoadTimeMs() {
        Task task = taskWith(Map.of("url", "https://example.com"));
        TaskResult result = worker.execute(task);

        assertEquals(320, result.getOutputData().get("loadTimeMs"));
    }

    @Test
    void sectionsHaveHeadingAndText() {
        Task task = taskWith(Map.of("url", "https://example.com", "title", "Test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) result.getOutputData().get("content");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) content.get("sections");

        for (Map<String, Object> section : sections) {
            assertNotNull(section.get("heading"));
            assertNotNull(section.get("text"));
        }
    }

    @Test
    void handlesNullUrl() {
        Map<String, Object> input = new HashMap<>();
        input.put("url", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("content"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) result.getOutputData().get("content");
        assertEquals("Untitled Page", content.get("title"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
