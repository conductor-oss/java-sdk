package requestaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergeResultsWorkerTest {

    private final MergeResultsWorker worker = new MergeResultsWorker();

    @Test
    void taskDefName() { assertEquals("agg_merge_results", worker.getTaskDefName()); }

    @Test
    void mergesAllInputs() {
        Map<String, Object> user = Map.of("name", "Alice");
        Map<String, Object> orders = Map.of("count", 2);
        Map<String, Object> recs = Map.of("items", "x");
        Task task = taskWith(Map.of("user", user, "orders", orders, "recommendations", recs));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> merged = (Map<String, Object>) result.getOutputData().get("merged");
        assertEquals(user, merged.get("user"));
        assertEquals(orders, merged.get("orders"));
        assertEquals(recs, merged.get("recommendations"));
    }

    @Test
    void handlesNullUser() {
        Map<String, Object> input = new HashMap<>();
        input.put("user", null);
        input.put("orders", Map.of("a", "b"));
        input.put("recommendations", Map.of("c", "d"));
        TaskResult result = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullOrders() {
        Map<String, Object> input = new HashMap<>();
        input.put("user", Map.of("name", "Alice"));
        input.put("orders", null);
        input.put("recommendations", Map.of());
        TaskResult result = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullRecommendations() {
        Map<String, Object> input = new HashMap<>();
        input.put("user", Map.of("name", "Alice"));
        input.put("orders", Map.of());
        input.put("recommendations", null);
        TaskResult result = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        TaskResult result = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("merged"));
    }

    @Test
    void mergedContainsThreeKeys() {
        TaskResult result = worker.execute(taskWith(Map.of(
                "user", Map.of("n", "a"), "orders", Map.of("o", "b"), "recommendations", Map.of("r", "c"))));
        @SuppressWarnings("unchecked")
        Map<String, Object> merged = (Map<String, Object>) result.getOutputData().get("merged");
        assertEquals(3, merged.size());
    }

    @Test
    void outputContainsMergedKey() {
        TaskResult result = worker.execute(taskWith(Map.of()));
        assertTrue(result.getOutputData().containsKey("merged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input)); return task;
    }
}
