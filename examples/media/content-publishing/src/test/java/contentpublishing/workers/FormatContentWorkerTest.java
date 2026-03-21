package contentpublishing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatContentWorkerTest {

    private final FormatContentWorker worker = new FormatContentWorker();

    @Test
    void taskDefName() {
        assertEquals("pub_format_content", worker.getTaskDefName());
    }

    @Test
    void formatsContentSuccessfully() {
        Task task = taskWith(Map.of(
                "contentId", "CNT-001",
                "contentType", "blog_post",
                "approved", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("https://cms.example.com/content/511-formatted",
                result.getOutputData().get("formattedUrl"));
    }

    @Test
    void outputContainsSeoMetadata() {
        Task task = taskWith(Map.of("contentId", "CNT-002", "contentType", "article"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, String> seo = (Map<String, String>) result.getOutputData().get("seoMetadata");
        assertEquals("Optimizing Cloud Costs", seo.get("metaTitle"));
        assertEquals("A guide to reducing cloud spend", seo.get("metaDescription"));
    }

    @Test
    void outputContainsFormats() {
        Task task = taskWith(Map.of("contentId", "CNT-003", "contentType", "blog_post"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> formats = (List<String>) result.getOutputData().get("formats");
        assertEquals(3, formats.size());
        assertTrue(formats.contains("html"));
        assertTrue(formats.contains("amp"));
        assertTrue(formats.contains("rss"));
    }

    @Test
    void handlesNullContentType() {
        Map<String, Object> input = new HashMap<>();
        input.put("contentId", "CNT-004");
        input.put("contentType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("formattedUrl"));
    }

    @Test
    void formattedUrlIsString() {
        Task task = taskWith(Map.of("contentId", "CNT-005", "contentType", "page"));
        TaskResult result = worker.execute(task);

        assertInstanceOf(String.class, result.getOutputData().get("formattedUrl"));
    }

    @Test
    void outputHasAllExpectedKeys() {
        Task task = taskWith(Map.of("contentId", "CNT-006", "contentType", "blog_post"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("formattedUrl"));
        assertTrue(result.getOutputData().containsKey("seoMetadata"));
        assertTrue(result.getOutputData().containsKey("formats"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
