package checkoutflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateCartWorkerTest {

    private ValidateCartWorker worker;

    @BeforeEach
    void setUp() {
        ValidateCartWorker.resetInventory();
        worker = new ValidateCartWorker();
    }

    @Test
    void taskDefName() {
        assertEquals("chk_validate_cart", worker.getTaskDefName());
    }

    @Test
    void validCartWithItems() {
        Task task = taskWith(Map.of("cartId", "cart-123", "userId", "usr-1",
                "items", List.of(
                        Map.of("sku", "LAPTOP-PRO", "name", "Laptop", "price", 1999.00, "qty", 1),
                        Map.of("sku", "USB-C-HUB", "name", "Hub", "price", 49.99, "qty", 2)
                )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        double subtotal = ((Number) result.getOutputData().get("subtotal")).doubleValue();
        assertEquals(2098.98, subtotal, 0.01); // 1999 + 49.99*2
        assertEquals(3, ((Number) result.getOutputData().get("itemCount")).intValue());
    }

    @Test
    void returnsSubtotal() {
        Task task = taskWith(Map.of("cartId", "cart-456", "userId", "usr-2",
                "items", List.of(
                        Map.of("sku", "ITEM-A", "name", "A", "price", 25.00, "qty", 4)
                )));
        TaskResult result = worker.execute(task);

        assertEquals(100.0, ((Number) result.getOutputData().get("subtotal")).doubleValue());
    }

    @Test
    void returnsItemCount() {
        Task task = taskWith(Map.of("cartId", "cart-789", "userId", "usr-3",
                "items", List.of(
                        Map.of("sku", "X", "name", "X", "price", 10.0, "qty", 3),
                        Map.of("sku", "Y", "name", "Y", "price", 5.0, "qty", 2)
                )));
        TaskResult result = worker.execute(task);

        assertEquals(5, ((Number) result.getOutputData().get("itemCount")).intValue());
    }

    @Test
    void outputHasExpectedKeys() {
        Task task = taskWith(Map.of("cartId", "cart-abc", "userId", "usr-4",
                "items", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1))));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("valid"));
        assertTrue(result.getOutputData().containsKey("subtotal"));
        assertTrue(result.getOutputData().containsKey("itemCount"));
        assertTrue(result.getOutputData().containsKey("items"));
    }

    @Test
    void zeroQuantityIsInvalid() {
        Task task = taskWith(Map.of("cartId", "cart-bad", "userId", "usr-5",
                "items", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 0))));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void negativePriceIsInvalid() {
        Task task = taskWith(Map.of("cartId", "cart-neg", "userId", "usr-6",
                "items", List.of(Map.of("sku", "A", "name", "A", "price", -5.0, "qty", 1))));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void couponPercentageDiscount() {
        Task task = taskWith(Map.of("cartId", "cart-coupon", "userId", "usr-7",
                "items", List.of(Map.of("sku", "A", "name", "A", "price", 100.0, "qty", 1)),
                "couponCode", "SAVE10"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(90.0, ((Number) result.getOutputData().get("subtotal")).doubleValue(), 0.01);
        assertEquals("SAVE10", result.getOutputData().get("discountApplied"));
        assertEquals(10.0, ((Number) result.getOutputData().get("discountAmount")).doubleValue(), 0.01);
    }

    @Test
    void couponFixedDiscount() {
        Task task = taskWith(Map.of("cartId", "cart-flat", "userId", "usr-8",
                "items", List.of(Map.of("sku", "A", "name", "A", "price", 50.0, "qty", 1)),
                "couponCode", "FLAT5"));
        TaskResult result = worker.execute(task);

        assertEquals(45.0, ((Number) result.getOutputData().get("subtotal")).doubleValue(), 0.01);
    }

    @Test
    void invalidCouponCode() {
        Task task = taskWith(Map.of("cartId", "cart-inv", "userId", "usr-9",
                "items", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1)),
                "couponCode", "INVALID"));
        TaskResult result = worker.execute(task);

        // Invalid coupon should not prevent cart validation
        assertNull(result.getOutputData().get("discountApplied"));
    }

    @Test
    void insufficientInventory() {
        ValidateCartWorker.setInventory("LIMITED-ITEM", 2);
        Task task = taskWith(Map.of("cartId", "cart-stock", "userId", "usr-10",
                "items", List.of(Map.of("sku", "LIMITED-ITEM", "name", "Limited", "price", 10.0, "qty", 5))));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void defaultCartWhenNoItemsProvided() {
        Task task = taskWith(Map.of("cartId", "cart-default", "userId", "usr-11"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(((Number) result.getOutputData().get("subtotal")).doubleValue() > 0);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
