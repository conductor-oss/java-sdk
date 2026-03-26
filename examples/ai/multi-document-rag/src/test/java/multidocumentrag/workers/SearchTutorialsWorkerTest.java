package multidocumentrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchTutorialsWorkerTest {

    private final SearchTutorialsWorker worker = new SearchTutorialsWorker();

    @Test
    void taskDefName() {
        assertEquals("mdrag_search_tutorials", worker.getTaskDefName());
    }

    @Test
    void returnsTwoResultsWithTutorialsSource() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, 0.2),
                "collection", "tutorials")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(2, results.size());
        for (Map<String, Object> r : results) {
            assertEquals("tutorials", r.get("source"));
            assertNotNull(r.get("text"));
            assertNotNull(r.get("score"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
