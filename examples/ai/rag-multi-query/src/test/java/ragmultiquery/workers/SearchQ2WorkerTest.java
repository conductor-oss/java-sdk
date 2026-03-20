package ragmultiquery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchQ2WorkerTest {

    private final SearchQ2Worker worker = new SearchQ2Worker();

    @Test
    void taskDefName() {
        assertEquals("mq_search_q2", worker.getTaskDefName());
    }

    @Test
    void returnsThreeResults() {
        Task task = taskWith(new HashMap<>(Map.of("query", "Why use Conductor for microservice coordination?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    void returnsDocumentsD1D7D9() {
        Task task = taskWith(new HashMap<>(Map.of("query", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) result.getOutputData().get("results");
        assertEquals("d1", results.get(0).get("id"));
        assertEquals("d7", results.get(1).get("id"));
        assertEquals("d9", results.get(2).get("id"));
    }

    @Test
    void d1OverlapsWithQ1() {
        Task task = taskWith(new HashMap<>(Map.of("query", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) result.getOutputData().get("results");
        assertEquals("d1", results.get(0).get("id"));
        assertTrue(results.get(0).get("text").contains("centralized control"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
