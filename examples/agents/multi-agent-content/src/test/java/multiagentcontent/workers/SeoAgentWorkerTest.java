package multiagentcontent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SeoAgentWorkerTest {

    private final SeoAgentWorker worker = new SeoAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("cc_seo_agent", worker.getTaskDefName());
    }

    @Test
    void returnsOptimizedArticle() {
        Task task = taskWith(Map.of("draft", "Original draft text", "topic", "DevOps"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String optimized = (String) result.getOutputData().get("optimizedArticle");
        assertNotNull(optimized);
        assertTrue(optimized.contains("Original draft text"));
        assertTrue(optimized.contains("SEO optimized"));
        assertTrue(optimized.contains("DevOps"));
    }

    @Test
    void returnsFourSuggestions() {
        Task task = taskWith(Map.of("draft", "Some article", "topic", "Microservices"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) result.getOutputData().get("suggestions");
        assertNotNull(suggestions);
        assertEquals(4, suggestions.size());
        assertTrue(suggestions.get(0).contains("Microservices"));
    }

    @Test
    void returnsSeoScore87() {
        Task task = taskWith(Map.of("draft", "Draft", "topic", "Testing"));
        TaskResult result = worker.execute(task);

        assertEquals(87, result.getOutputData().get("seoScore"));
    }

    @Test
    void suggestionsContainTopic() {
        Task task = taskWith(Map.of("draft", "Content here", "topic", "Kubernetes"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) result.getOutputData().get("suggestions");
        for (String suggestion : suggestions) {
            assertTrue(suggestion.contains("Kubernetes"),
                    "Each suggestion should reference the topic: " + suggestion);
        }
    }

    @Test
    void handlesNullDraft() {
        Map<String, Object> input = new HashMap<>();
        input.put("draft", null);
        input.put("topic", "AI");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("optimizedArticle"));
    }

    @Test
    void handlesMissingTopic() {
        Task task = taskWith(Map.of("draft", "Some draft"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) result.getOutputData().get("suggestions");
        assertNotNull(suggestions);
        assertEquals(4, suggestions.size());
    }

    @Test
    void handlesBlankDraft() {
        Task task = taskWith(Map.of("draft", "   ", "topic", "Test"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("optimizedArticle"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
