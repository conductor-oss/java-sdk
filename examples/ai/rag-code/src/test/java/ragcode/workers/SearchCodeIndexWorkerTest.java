package ragcode.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchCodeIndexWorkerTest {

    private final SearchCodeIndexWorker worker = new SearchCodeIndexWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_search_code_index", worker.getTaskDefName());
    }

    @Test
    void returnsThreeCodeSnippets() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.25, -0.15, 0.68),
                "codeFilter", Map.of("language", "java")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> snippets = (List<Map<String, Object>>) result.getOutputData().get("codeSnippets");
        assertNotNull(snippets);
        assertEquals(3, snippets.size());
    }

    @Test
    void snippetsHaveRequiredFields() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> snippets = (List<Map<String, Object>>) result.getOutputData().get("codeSnippets");

        for (Map<String, Object> snippet : snippets) {
            assertNotNull(snippet.get("id"), "snippet must have id");
            assertNotNull(snippet.get("file"), "snippet must have file");
            assertNotNull(snippet.get("line"), "snippet must have line");
            assertNotNull(snippet.get("signature"), "snippet must have signature");
            assertNotNull(snippet.get("body"), "snippet must have body");
            assertNotNull(snippet.get("astType"), "snippet must have astType");
            assertNotNull(snippet.get("score"), "snippet must have score");
        }
    }

    @Test
    void snippetsAreSortedByScoreDescending() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> snippets = (List<Map<String, Object>>) result.getOutputData().get("codeSnippets");

        double prevScore = 1.0;
        for (Map<String, Object> snippet : snippets) {
            double score = ((Number) snippet.get("score")).doubleValue();
            assertTrue(score <= prevScore, "snippets should be sorted by score descending");
            prevScore = score;
        }
    }

    @Test
    void topSnippetHasExpectedId() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> snippets = (List<Map<String, Object>>) result.getOutputData().get("codeSnippets");

        assertEquals("snippet-001", snippets.get(0).get("id"));
        assertEquals(0.95, ((Number) snippets.get(0).get("score")).doubleValue());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("codeSnippets"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
