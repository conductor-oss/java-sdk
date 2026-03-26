package productcatalog.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndexProductWorkerTest {

    private final IndexProductWorker worker = new IndexProductWorker();

    @Test
    void taskDefName() {
        assertEquals("prd_index", worker.getTaskDefName());
    }

    @Test
    void indexesProduct() {
        Task task = taskWith(Map.of("productId", "prod-1", "name", "Widget", "category", "Tools"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("indexed"));
    }

    @Test
    void outputContainsIndexedAt() {
        Task task = taskWith(Map.of("productId", "prod-1", "name", "Widget", "category", "Tools"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("indexedAt"));
    }

    @Test
    void handlesNullName() {
        Task task = taskWith(new HashMap<>(Map.of("productId", "prod-1", "category", "General")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullCategory() {
        Task task = taskWith(new HashMap<>(Map.of("productId", "prod-1", "name", "Test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void indexedIsAlwaysTrue() {
        Task task = taskWith(Map.of("productId", "prod-x", "name", "N", "category", "C"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("indexed"));
    }

    @Test
    void outputHasExpectedKeys() {
        Task task = taskWith(Map.of("productId", "prod-1", "name", "A", "category", "B"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("indexed"));
        assertTrue(result.getOutputData().containsKey("indexedAt"));
    }

    @Test
    void handlesEmptyInput() {
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
