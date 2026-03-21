package recommendationengine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectBehaviorWorkerTest {

    private final CollectBehaviorWorker worker = new CollectBehaviorWorker();

    @Test
    void taskDefName() {
        assertEquals("rec_collect_behavior", worker.getTaskDefName());
    }

    @Test
    void collectsViewedProducts() {
        Task task = taskWith(Map.of("userId", "U-1", "context", "homepage"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> viewed = (List<String>) result.getOutputData().get("viewedProducts");
        assertNotNull(viewed);
        assertTrue(viewed.size() >= 4);
        assertTrue(viewed.contains("SKU-101"));
    }

    @Test
    void collectsPurchasedProducts() {
        Task task = taskWith(Map.of("userId", "U-2", "context", "cart"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> purchased = (List<String>) result.getOutputData().get("purchasedProducts");
        assertNotNull(purchased);
        assertTrue(purchased.contains("SKU-101"));
        assertTrue(purchased.contains("SKU-050"));
    }

    @Test
    void outputContainsBrowsingCategories() {
        Task task = taskWith(Map.of("userId", "U-3", "context", "product_page"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> categories = (List<String>) result.getOutputData().get("browsingCategories");
        assertNotNull(categories);
        assertTrue(categories.contains("electronics"));
        assertTrue(categories.contains("audio"));
    }

    @Test
    void outputContainsSessionCount() {
        Task task = taskWith(Map.of("userId", "U-4", "context", "homepage"));
        TaskResult result = worker.execute(task);

        assertEquals(14, result.getOutputData().get("sessionCount"));
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", null);
        input.put("context", "homepage");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullContext() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", "U-5");
        input.put("context", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("viewedProducts"));
        assertNotNull(result.getOutputData().get("purchasedProducts"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
