package requestaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FetchRecommendationsWorkerTest {

    private final FetchRecommendationsWorker worker = new FetchRecommendationsWorker();

    @Test
    void taskDefName() { assertEquals("agg_fetch_recommendations", worker.getTaskDefName()); }

    @Test
    void fetchesRecommendations() {
        TaskResult result = worker.execute(taskWith(Map.of("userId", "user-42")));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("items"));
        assertEquals(2, result.getOutputData().get("count"));
    }

    @Test
    void returnsTwoItems() {
        TaskResult result = worker.execute(taskWith(Map.of("userId", "u1")));
        @SuppressWarnings("unchecked")
        List<String> items = (List<String>) result.getOutputData().get("items");
        assertEquals(2, items.size());
        assertTrue(items.contains("Widget Pro"));
        assertTrue(items.contains("Gadget X"));
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>(); input.put("userId", null);
        TaskResult result = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        TaskResult result = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void isDeterministic() {
        TaskResult r1 = worker.execute(taskWith(Map.of("userId", "a")));
        TaskResult r2 = worker.execute(taskWith(Map.of("userId", "b")));
        assertEquals(r1.getOutputData().get("count"), r2.getOutputData().get("count"));
    }

    @Test
    void countMatchesItemsSize() {
        TaskResult result = worker.execute(taskWith(Map.of("userId", "u1")));
        @SuppressWarnings("unchecked")
        List<String> items = (List<String>) result.getOutputData().get("items");
        assertEquals(items.size(), result.getOutputData().get("count"));
    }

    @Test
    void outputContainsBothFields() {
        TaskResult result = worker.execute(taskWith(Map.of("userId", "u")));
        assertTrue(result.getOutputData().containsKey("items"));
        assertTrue(result.getOutputData().containsKey("count"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input)); return task;
    }
}
