package ragmultiquery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchQ3WorkerTest {

    private final SearchQ3Worker worker = new SearchQ3Worker();

    @Test
    void taskDefName() {
        assertEquals("mq_search_q3", worker.getTaskDefName());
    }

    @Test
    void returnsTwoResults() {
        Task task = taskWith(new HashMap<>(Map.of("query", "Advantages of orchestration vs choreography patterns")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void returnsDocumentsD4AndD11() {
        Task task = taskWith(new HashMap<>(Map.of("query", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) result.getOutputData().get("results");
        assertEquals("d4", results.get(0).get("id"));
        assertEquals("d11", results.get(1).get("id"));
    }

    @Test
    void d4OverlapsWithQ1() {
        Task task = taskWith(new HashMap<>(Map.of("query", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) result.getOutputData().get("results");
        assertEquals("d4", results.get(0).get("id"));
        assertTrue(results.get(0).get("text").contains("retry"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
