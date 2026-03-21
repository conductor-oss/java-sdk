package serviceorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AddToCartWorkerTest {

    private final AddToCartWorker worker = new AddToCartWorker();

    @Test
    void taskDefName() {
        assertEquals("so_add_to_cart", worker.getTaskDefName());
    }

    @Test
    void addsItemToCart() {
        Task task = taskWith(Map.of("userId", "user-42", "product", Map.of("id", "PROD-100"), "quantity", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CART-7891", result.getOutputData().get("cartId"));
        assertEquals(1, result.getOutputData().get("items"));
        assertEquals(159.98, ((Number) result.getOutputData().get("total")).doubleValue(), 0.001);
    }

    @Test
    void defaultsQuantityToOne() {
        Task task = taskWith(Map.of("userId", "user-42", "product", Map.of("id", "PROD-100")));
        TaskResult result = worker.execute(task);

        assertEquals(79.99, ((Number) result.getOutputData().get("total")).doubleValue(), 0.001);
    }

    @Test
    void calculatesTotalForQuantityThree() {
        Task task = taskWith(Map.of("userId", "user-42", "product", Map.of("id", "PROD-100"), "quantity", 3));
        TaskResult result = worker.execute(task);

        assertEquals(239.97, ((Number) result.getOutputData().get("total")).doubleValue(), 0.01);
    }

    @Test
    void returnsCartId() {
        Task task = taskWith(Map.of("userId", "user-42", "quantity", 1));
        TaskResult result = worker.execute(task);

        assertEquals("CART-7891", result.getOutputData().get("cartId"));
    }

    @Test
    void handlesQuantityOfOne() {
        Task task = taskWith(Map.of("userId", "user-42", "quantity", 1));
        TaskResult result = worker.execute(task);

        assertEquals(79.99, ((Number) result.getOutputData().get("total")).doubleValue(), 0.001);
    }

    @Test
    void handlesMissingQuantity() {
        Task task = taskWith(Map.of("userId", "user-42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(79.99, ((Number) result.getOutputData().get("total")).doubleValue(), 0.001);
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("userId", "user-42", "quantity", 1));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("cartId"));
        assertTrue(result.getOutputData().containsKey("items"));
        assertTrue(result.getOutputData().containsKey("total"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
