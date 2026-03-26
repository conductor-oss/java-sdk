package ragmultiquery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchQ1WorkerTest {

    private final SearchQ1Worker worker = new SearchQ1Worker();

    @Test
    void taskDefName() {
        assertEquals("mq_search_q1", worker.getTaskDefName());
    }

    @Test
    void returnsTwoResults() {
        Task task = taskWith(new HashMap<>(Map.of("query", "What are the benefits of workflow orchestration?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void returnsDocumentsD1AndD4() {
        Task task = taskWith(new HashMap<>(Map.of("query", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) result.getOutputData().get("results");
        assertEquals("d1", results.get(0).get("id"));
        assertEquals("d4", results.get(1).get("id"));
    }

    @Test
    void resultsContainText() {
        Task task = taskWith(new HashMap<>(Map.of("query", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) result.getOutputData().get("results");
        assertTrue(results.get(0).get("text").contains("centralized control"));
        assertTrue(results.get(1).get("text").contains("retry"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
