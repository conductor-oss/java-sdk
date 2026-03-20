package productcatalog.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateProductWorkerTest {

    private final ValidateProductWorker worker = new ValidateProductWorker();

    @Test
    void taskDefName() {
        assertEquals("prd_validate", worker.getTaskDefName());
    }

    @Test
    void validWhenSkuAndPricePresent() {
        Task task = taskWith(Map.of("productId", "prod-1", "sku", "SKU-A", "price", 49.99));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void invalidWhenSkuMissing() {
        Task task = taskWith(new HashMap<>(Map.of("productId", "prod-1", "price", 49.99)));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void invalidWhenPriceZero() {
        Task task = taskWith(Map.of("productId", "prod-1", "sku", "SKU-A", "price", 0));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void invalidWhenPriceNegative() {
        Task task = taskWith(Map.of("productId", "prod-1", "sku", "SKU-A", "price", -10.0));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void handlesPriceAsString() {
        Task task = taskWith(Map.of("productId", "prod-1", "sku", "SKU-A", "price", "29.99"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void invalidWhenSkuBlank() {
        Task task = taskWith(Map.of("productId", "prod-1", "sku", "   ", "price", 10.0));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void outputContainsValidatedAt() {
        Task task = taskWith(Map.of("productId", "prod-1", "sku", "SKU-X", "price", 5.0));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("validatedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
