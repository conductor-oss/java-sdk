package multiagentsupport.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeSearchWorkerTest {

    private final KnowledgeSearchWorker worker = new KnowledgeSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("cs_knowledge_search", worker.getTaskDefName());
    }

    @Test
    void returnsThreeArticles() {
        Task task = taskWith(Map.of(
                "subject", "Login error",
                "description", "App crashes",
                "keywords", List.of("error", "crash")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> articles = (List<Map<String, Object>>) result.getOutputData().get("kbArticles");
        assertNotNull(articles);
        assertEquals(3, articles.size());
    }

    @Test
    void articlesHaveRequiredFields() {
        Task task = taskWith(Map.of(
                "subject", "Error",
                "description", "Desc",
                "keywords", List.of("error")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> articles = (List<Map<String, Object>>) result.getOutputData().get("kbArticles");
        for (Map<String, Object> article : articles) {
            assertTrue(article.containsKey("id"));
            assertTrue(article.containsKey("title"));
            assertTrue(article.containsKey("relevance"));
            assertTrue(article.containsKey("excerpt"));
        }
    }

    @Test
    void firstArticleHasHighestRelevance() {
        Task task = taskWith(Map.of(
                "subject", "Bug",
                "description", "Broken",
                "keywords", List.of("bug")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> articles = (List<Map<String, Object>>) result.getOutputData().get("kbArticles");
        assertEquals(0.95, articles.get(0).get("relevance"));
        assertEquals("KB-1001", articles.get(0).get("id"));
    }

    @Test
    void returnsSearchTime() {
        Task task = taskWith(Map.of(
                "subject", "Test",
                "description", "Test",
                "keywords", List.of("test")));
        TaskResult result = worker.execute(task);

        assertEquals("120ms", result.getOutputData().get("searchTime"));
    }

    @Test
    void handlesNullKeywords() {
        Map<String, Object> input = new HashMap<>();
        input.put("subject", "Test");
        input.put("description", "Test");
        input.put("keywords", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("kbArticles"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("kbArticles"));
        assertNotNull(result.getOutputData().get("searchTime"));
    }

    @Test
    void handlesNullSubjectAndDescription() {
        Map<String, Object> input = new HashMap<>();
        input.put("subject", null);
        input.put("description", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
