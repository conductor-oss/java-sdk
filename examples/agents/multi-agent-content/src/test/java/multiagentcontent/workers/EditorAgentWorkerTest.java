package multiagentcontent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EditorAgentWorkerTest {

    private final EditorAgentWorker worker = new EditorAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("cc_editor_agent", worker.getTaskDefName());
    }

    @Test
    void returnsPolishedArticle() {
        Task task = taskWith(Map.of(
                "article", "# Great Topic\nSome article text here.",
                "seoSuggestions", List.of("Add meta", "Fix keywords")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String polished = (String) result.getOutputData().get("polishedArticle");
        assertNotNull(polished);
        assertTrue(polished.contains("Some article text here."));
        assertTrue(polished.contains("Editor notes"));
    }

    @Test
    void returnsWordCount() {
        Task task = taskWith(Map.of("article", "# Title\nOne two three four five."));
        TaskResult result = worker.execute(task);

        Integer wordCount = (Integer) result.getOutputData().get("wordCount");
        assertNotNull(wordCount);
        assertTrue(wordCount > 0);
    }

    @Test
    void wordCountMatchesPolishedArticle() {
        Task task = taskWith(Map.of(
                "article", "# My Title\nWord word word word word.",
                "seoSuggestions", List.of("Suggestion 1")));
        TaskResult result = worker.execute(task);

        String polished = (String) result.getOutputData().get("polishedArticle");
        int expected = polished.split("\\s+").length;
        assertEquals(expected, result.getOutputData().get("wordCount"));
    }

    @Test
    void returnsMetadataWithTitle() {
        Task task = taskWith(Map.of("article", "# AI in Healthcare\nContent here."));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getOutputData().get("metadata");
        assertNotNull(metadata);
        assertEquals("AI in Healthcare", metadata.get("title"));
        assertEquals("Content Creation Pipeline", metadata.get("author"));
    }

    @Test
    void metadataContainsTags() {
        Task task = taskWith(Map.of("article", "# Topic\nText."));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getOutputData().get("metadata");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) metadata.get("tags");
        assertNotNull(tags);
        assertEquals(3, tags.size());
        assertTrue(tags.contains("technology"));
    }

    @Test
    void readTimeComputedDeterministically() {
        // Build an article with a known word count to test readTime
        StringBuilder sb = new StringBuilder("# Test Article\n");
        for (int i = 0; i < 300; i++) {
            sb.append("word ");
        }
        Task task = taskWith(Map.of("article", sb.toString()));
        TaskResult result = worker.execute(task);

        String polished = (String) result.getOutputData().get("polishedArticle");
        int wordCount = polished.split("\\s+").length;
        int expectedMinutes = (int) Math.ceil(wordCount / 200.0);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getOutputData().get("metadata");
        assertEquals(expectedMinutes + " min", metadata.get("readTime"));
    }

    @Test
    void returnsReadabilityGrade() {
        Task task = taskWith(Map.of("article", "# Title\nSome content."));
        TaskResult result = worker.execute(task);

        assertEquals(8.2, result.getOutputData().get("readabilityGrade"));
    }

    @Test
    void handlesNullArticle() {
        Map<String, Object> input = new HashMap<>();
        input.put("article", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("polishedArticle"));
    }

    @Test
    void handlesMissingSeoSuggestions() {
        Task task = taskWith(Map.of("article", "# Title\nContent."));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("polishedArticle"));
    }

    @Test
    void handlesTitleExtractionWithoutHeading() {
        Task task = taskWith(Map.of("article", "No heading here, just text."));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getOutputData().get("metadata");
        assertEquals("Comprehensive Guide", metadata.get("title"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
