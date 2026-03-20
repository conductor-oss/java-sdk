package serviceorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutWorkerTest {

    private final CheckoutWorker worker = new CheckoutWorker();

    @Test
    void taskDefName() {
        assertEquals("so_checkout", worker.getTaskDefName());
    }

    @Test
    void processesCheckout() {
        Task task = taskWith(Map.of("userId", "user-42", "cartId", "CART-7891", "total", 159.98));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-20240301-001", result.getOutputData().get("orderId"));
        assertEquals(159.98, ((Number) result.getOutputData().get("total")).doubleValue(), 0.001);
        assertEquals("confirmed", result.getOutputData().get("orderStatus"));
        assertEquals("2024-03-05", result.getOutputData().get("estimatedDelivery"));
    }

    @Test
    void preservesTotalFromInput() {
        Task task = taskWith(Map.of("userId", "user-42", "cartId", "CART-001", "total", 299.99));
        TaskResult result = worker.execute(task);

        assertEquals(299.99, ((Number) result.getOutputData().get("total")).doubleValue(), 0.001);
    }

    @Test
    void returnsConfirmedStatus() {
        Task task = taskWith(Map.of("userId", "user-42", "cartId", "CART-001", "total", 50.0));
        TaskResult result = worker.execute(task);

        assertEquals("confirmed", result.getOutputData().get("orderStatus"));
    }

    @Test
    void returnsEstimatedDelivery() {
        Task task = taskWith(Map.of("userId", "user-42", "cartId", "CART-001", "total", 50.0));
        TaskResult result = worker.execute(task);

        assertEquals("2024-03-05", result.getOutputData().get("estimatedDelivery"));
    }

    @Test
    void handlesZeroTotal() {
        Task task = taskWith(Map.of("userId", "user-42", "cartId", "CART-001", "total", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, ((Number) result.getOutputData().get("total")).doubleValue(), 0.001);
    }

    @Test
    void handlesMissingTotal() {
        Task task = taskWith(Map.of("userId", "user-42", "cartId", "CART-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, ((Number) result.getOutputData().get("total")).doubleValue(), 0.001);
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("userId", "user-42", "cartId", "CART-001", "total", 100.0));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("orderId"));
        assertTrue(result.getOutputData().containsKey("total"));
        assertTrue(result.getOutputData().containsKey("orderStatus"));
        assertTrue(result.getOutputData().containsKey("estimatedDelivery"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
