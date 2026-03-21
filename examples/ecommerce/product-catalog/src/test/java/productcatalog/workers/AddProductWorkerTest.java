package productcatalog.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AddProductWorkerTest {

    private final AddProductWorker worker = new AddProductWorker();

    @Test
    void taskDefName() {
        assertEquals("prd_add_product", worker.getTaskDefName());
    }

    @Test
    void createsProductWithId() {
        Task task = taskWith(Map.of("sku", "SKU-100", "name", "Widget"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("productId"));
        assertTrue(result.getOutputData().get("productId").toString().startsWith("prod-"));
    }

    @Test
    void outputContainsCreatedAt() {
        Task task = taskWith(Map.of("sku", "SKU-200", "name", "Gadget"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("createdAt"));
    }

    @Test
    void productIdIsUnique() {
        Task task1 = taskWith(Map.of("sku", "A", "name", "A"));
        Task task2 = taskWith(Map.of("sku", "B", "name", "B"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertNotEquals(r1.getOutputData().get("productId"), r2.getOutputData().get("productId"));
    }

    @Test
    void handlesNullName() {
        Task task = taskWith(new HashMap<>(Map.of("sku", "SKU-300")));
        task.getInputData().put("name", null);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("productId"));
    }

    @Test
    void handlesNullSku() {
        Task task = taskWith(new HashMap<>(Map.of("name", "Test")));
        task.getInputData().put("sku", null);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputHasExpectedKeys() {
        Task task = taskWith(Map.of("sku", "SKU-400", "name", "Test Product"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("productId"));
        assertTrue(result.getOutputData().containsKey("createdAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
