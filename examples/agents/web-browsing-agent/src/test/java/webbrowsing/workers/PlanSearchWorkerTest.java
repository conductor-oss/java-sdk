package webbrowsing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanSearchWorkerTest {

    private final PlanSearchWorker worker = new PlanSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("wb_plan_search", worker.getTaskDefName());
    }

    @Test
    void returnsThreeSearchQueries() {
        Task task = taskWith(Map.of("question", "What is Conductor?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> queries = (List<String>) result.getOutputData().get("searchQueries");
        assertNotNull(queries);
        assertEquals(3, queries.size());
    }

    @Test
    void queriesIncludeOriginalQuestion() {
        String question = "How does Conductor handle retries?";
        Task task = taskWith(Map.of("question", question));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> queries = (List<String>) result.getOutputData().get("searchQueries");
        assertTrue(queries.contains(question));
    }

    @Test
    void returnsGoogleAsSearchEngine() {
        Task task = taskWith(Map.of("question", "Conductor features"));
        TaskResult result = worker.execute(task);

        assertEquals("google", result.getOutputData().get("searchEngine"));
    }

    @Test
    void returnsMultiQueryStrategy() {
        Task task = taskWith(Map.of("question", "Conductor features"));
        TaskResult result = worker.execute(task);

        assertEquals("multi-query", result.getOutputData().get("strategy"));
    }

    @Test
    void handlesNullQuestion() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> queries = (List<String>) result.getOutputData().get("searchQueries");
        assertNotNull(queries);
        assertEquals(3, queries.size());
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("searchQueries"));
    }

    @Test
    void handlesBlankQuestion() {
        Task task = taskWith(Map.of("question", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> queries = (List<String>) result.getOutputData().get("searchQueries");
        assertNotNull(queries);
        assertEquals(3, queries.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
