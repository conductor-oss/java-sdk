package serviceorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CatalogLookupWorkerTest {

    private final CatalogLookupWorker worker = new CatalogLookupWorker();

    @Test
    void taskDefName() {
        assertEquals("so_catalog_lookup", worker.getTaskDefName());
    }

    @Test
    void looksUpProduct() {
        Task task = taskWith(Map.of("productId", "PROD-100", "authToken", "jwt-token-abc123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertEquals("PROD-100", product.get("id"));
        assertEquals("Wireless Headphones", product.get("name"));
        assertEquals(79.99, product.get("price"));
        assertEquals(true, product.get("available"));
    }

    @Test
    void usesProvidedProductId() {
        Task task = taskWith(Map.of("productId", "PROD-999", "authToken", "token"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertEquals("PROD-999", product.get("id"));
    }

    @Test
    void productIsAvailable() {
        Task task = taskWith(Map.of("productId", "PROD-100", "authToken", "token"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertTrue((Boolean) product.get("available"));
    }

    @Test
    void handlesNullProductId() {
        Map<String, Object> input = new HashMap<>();
        input.put("productId", null);
        input.put("authToken", "token");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertEquals("UNKNOWN", product.get("id"));
    }

    @Test
    void handlesMissingProductId() {
        Task task = taskWith(Map.of("authToken", "token"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertEquals("UNKNOWN", product.get("id"));
    }

    @Test
    void productHasCorrectPrice() {
        Task task = taskWith(Map.of("productId", "PROD-100", "authToken", "token"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.getOutputData().get("product");
        assertEquals(79.99, ((Number) product.get("price")).doubleValue(), 0.001);
    }

    @Test
    void outputContainsProductKey() {
        Task task = taskWith(Map.of("productId", "PROD-100", "authToken", "token"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("product"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
