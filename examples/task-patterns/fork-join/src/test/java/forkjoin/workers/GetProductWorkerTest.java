package forkjoin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GetProductWorkerTest {

    private final GetProductWorker worker = new GetProductWorker();

    @Test
    void taskDefName() {
        assertEquals("fj_get_product", worker.getTaskDefName());
    }

    @Test
    void returnsKnownProductDetails() {
        Task task = taskWith(Map.of("productId", "PROD-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertNotNull(product);
        assertEquals("PROD-001", product.get("id"));
        assertEquals("Wireless Headphones", product.get("name"));
        assertEquals(79.99, product.get("price"));
        assertEquals("Electronics", product.get("category"));
    }

    @Test
    void returnsAnotherKnownProduct() {
        Task task = taskWith(Map.of("productId", "PROD-003"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertEquals("USB-C Hub", product.get("name"));
        assertEquals(49.99, product.get("price"));
        assertEquals("Accessories", product.get("category"));
    }

    @Test
    void handlesCustomProductId() {
        Task task = taskWith(Map.of("productId", "PROD-999"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertEquals("PROD-999", product.get("id"));
        // Unknown product gets deterministic fallback
        assertEquals("Product PROD-999", product.get("name"));
        assertEquals("General", product.get("category"));
        assertNotNull(product.get("price"));
    }

    @Test
    void defaultsProductIdWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertEquals("UNKNOWN", product.get("id"));
    }

    @Test
    void defaultsProductIdWhenBlank() {
        Task task = taskWith(Map.of("productId", "   "));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertEquals("UNKNOWN", product.get("id"));
    }

    @Test
    void defaultsProductIdWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("productId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertEquals("UNKNOWN", product.get("id"));
    }

    @Test
    void productContainsAllExpectedFields() {
        Task task = taskWith(Map.of("productId", "PROD-001"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertEquals(4, product.size());
        assertTrue(product.containsKey("id"));
        assertTrue(product.containsKey("name"));
        assertTrue(product.containsKey("price"));
        assertTrue(product.containsKey("category"));
    }

    @Test
    void outputIsDeterministic() {
        Task task1 = taskWith(Map.of("productId", "PROD-XYZ"));
        Task task2 = taskWith(Map.of("productId", "PROD-XYZ"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("product"),
                result2.getOutputData().get("product"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
