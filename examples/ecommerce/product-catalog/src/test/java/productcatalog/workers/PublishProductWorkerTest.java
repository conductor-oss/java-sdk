package productcatalog.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PublishProductWorkerTest {

    private final PublishProductWorker worker = new PublishProductWorker();

    @Test
    void taskDefName() {
        assertEquals("prd_publish", worker.getTaskDefName());
    }

    @Test
    void publishesProduct() {
        Task task = taskWith(Map.of("productId", "prod-abc"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
    }

    @Test
    void outputContainsPublishedAt() {
        Task task = taskWith(Map.of("productId", "prod-abc"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("publishedAt"));
    }

    @Test
    void urlContainsProductId() {
        Task task = taskWith(Map.of("productId", "prod-xyz"));
        TaskResult result = worker.execute(task);

        assertEquals("/products/prod-xyz", result.getOutputData().get("url"));
    }

    @Test
    void handlesNullProductId() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
    }

    @Test
    void outputContainsUrl() {
        Task task = taskWith(Map.of("productId", "prod-test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().get("url").toString().startsWith("/products/"));
    }

    @Test
    void outputHasExpectedKeys() {
        Task task = taskWith(Map.of("productId", "prod-1"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("published"));
        assertTrue(result.getOutputData().containsKey("publishedAt"));
        assertTrue(result.getOutputData().containsKey("url"));
    }

    @Test
    void publishedIsAlwaysTrue() {
        Task task = taskWith(Map.of("productId", "prod-any"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("published"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
