package multiagentcontent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PublishWorkerTest {

    private final PublishWorker worker = new PublishWorker();

    @Test
    void taskDefName() {
        assertEquals("cc_publish", worker.getTaskDefName());
    }

    @Test
    void returnsPublishResult() {
        Task task = taskWith(Map.of(
                "finalArticle", "The final article content.",
                "metadata", Map.of("title", "AI in Healthcare", "author", "Pipeline"),
                "seoScore", 87));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("https://content.example.com/articles/ai-in-healthcare",
                result.getOutputData().get("url"));
        assertEquals("2025-01-15T10:30:00Z", result.getOutputData().get("publishedAt"));
        assertEquals("live", result.getOutputData().get("status"));
    }

    @Test
    void urlDerivedFromTitle() {
        Task task = taskWith(Map.of(
                "finalArticle", "Content",
                "metadata", Map.of("title", "Cloud Computing Best Practices"),
                "seoScore", 90));
        TaskResult result = worker.execute(task);

        String url = (String) result.getOutputData().get("url");
        assertTrue(url.contains("cloud-computing-best-practices"));
    }

    @Test
    void fixedTimestamp() {
        Task task = taskWith(Map.of(
                "finalArticle", "Article",
                "metadata", Map.of("title", "Test")));
        TaskResult result = worker.execute(task);

        assertEquals("2025-01-15T10:30:00Z", result.getOutputData().get("publishedAt"));
    }

    @Test
    void statusIsLive() {
        Task task = taskWith(Map.of(
                "finalArticle", "Article",
                "metadata", Map.of("title", "Test")));
        TaskResult result = worker.execute(task);

        assertEquals("live", result.getOutputData().get("status"));
    }

    @Test
    void handlesNullMetadata() {
        Map<String, Object> input = new HashMap<>();
        input.put("finalArticle", "Content");
        input.put("metadata", null);
        input.put("seoScore", 80);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String url = (String) result.getOutputData().get("url");
        assertTrue(url.contains("article"));
    }

    @Test
    void handlesMissingMetadata() {
        Task task = taskWith(Map.of("finalArticle", "Content"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("url"));
        assertEquals("live", result.getOutputData().get("status"));
    }

    @Test
    void urlSanitizesSpecialChars() {
        Task task = taskWith(Map.of(
                "finalArticle", "Content",
                "metadata", Map.of("title", "AI & ML: The Future!")));
        TaskResult result = worker.execute(task);

        String url = (String) result.getOutputData().get("url");
        // Check only the slug portion after the last /
        String slug = url.substring(url.lastIndexOf('/') + 1);
        assertFalse(slug.contains("&"));
        assertFalse(slug.contains("!"));
        assertFalse(slug.contains(":"));
        assertTrue(slug.matches("[a-z0-9]+(-[a-z0-9]+)*"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
