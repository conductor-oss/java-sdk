package multiagentcontent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WriterAgentWorkerTest {

    private final WriterAgentWorker worker = new WriterAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("cc_writer_agent", worker.getTaskDefName());
    }

    @Test
    void returnsArticleAndWordCount() {
        Task task = taskWith(Map.of(
                "topic", "AI in Healthcare",
                "facts", List.of("Fact 1", "Fact 2", "Fact 3", "Fact 4"),
                "sources", List.of("Source A", "Source B", "Source C"),
                "wordCount", 1000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String article = (String) result.getOutputData().get("article");
        assertNotNull(article);
        assertTrue(article.contains("AI in Healthcare"));
        assertTrue(article.contains("# AI in Healthcare"));

        Integer draftWordCount = (Integer) result.getOutputData().get("draftWordCount");
        assertNotNull(draftWordCount);
        assertTrue(draftWordCount > 0);
    }

    @Test
    void articleContainsFacts() {
        List<String> facts = List.of("First fact here", "Second fact here", "Third fact", "Fourth fact");
        Task task = taskWith(Map.of("topic", "Testing", "facts", facts, "sources", List.of()));
        TaskResult result = worker.execute(task);

        String article = (String) result.getOutputData().get("article");
        assertTrue(article.contains("First fact here"));
        assertTrue(article.contains("Second fact here"));
    }

    @Test
    void articleContainsSources() {
        List<String> sources = List.of("Journal of Testing", "Test Report 2025");
        Task task = taskWith(Map.of("topic", "QA", "facts", List.of(), "sources", sources));
        TaskResult result = worker.execute(task);

        String article = (String) result.getOutputData().get("article");
        assertTrue(article.contains("Journal of Testing"));
        assertTrue(article.contains("Test Report 2025"));
    }

    @Test
    void wordCountMatchesArticle() {
        Task task = taskWith(Map.of(
                "topic", "Cloud",
                "facts", List.of("F1", "F2", "F3", "F4"),
                "sources", List.of("S1")));
        TaskResult result = worker.execute(task);

        String article = (String) result.getOutputData().get("article");
        int expected = article.split("\\s+").length;
        assertEquals(expected, result.getOutputData().get("draftWordCount"));
    }

    @Test
    void handlesStringWordCount() {
        Task task = taskWith(Map.of("topic", "AI", "wordCount", "500"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("article"));
    }

    @Test
    void handlesNullFacts() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", "Null test");
        input.put("facts", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("article"));
    }

    @Test
    void handlesMissingTopic() {
        Task task = taskWith(Map.of("facts", List.of("A fact"), "sources", List.of("A source")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String article = (String) result.getOutputData().get("article");
        assertTrue(article.contains("general topic"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
