package correctiverag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebSearchWorkerTest {

    private final WebSearchWorker worker = new WebSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_web_search", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfullyRegardlessOfNetwork() {
        Task task = taskWith(new HashMap<>(Map.of("question", "Conductor pricing")));
        TaskResult result = worker.execute(task);

        // Worker should always complete (network errors are handled gracefully)
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("webResults"),
                "webResults key must always be present");
    }

    @Test
    void webResultsContainTitleAndSnippetWhenPresent() {
        Task task = taskWith(new HashMap<>(Map.of("question", "Conductor pricing")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> webResults = (List<Map<String, String>>) result.getOutputData().get("webResults");

        // If results were returned (depends on network), verify structure
        for (Map<String, String> webResult : webResults) {
            assertNotNull(webResult.get("title"), "Each result should have a title");
            assertNotNull(webResult.get("snippet"), "Each result should have a snippet");
            assertFalse(webResult.get("title").isEmpty(), "Title should not be empty");
            assertFalse(webResult.get("snippet").isEmpty(), "Snippet should not be empty");
        }
    }

    @Test
    void returnsAtMostThreeResults() {
        Task task = taskWith(new HashMap<>(Map.of("question", "workflow orchestration platform")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> webResults = (List<Map<String, String>>) result.getOutputData().get("webResults");
        assertTrue(webResults.size() <= 3, "Should return at most 3 results");
    }

    @Test
    void handlesEmptyQuestion() {
        Task task = taskWith(new HashMap<>());
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
