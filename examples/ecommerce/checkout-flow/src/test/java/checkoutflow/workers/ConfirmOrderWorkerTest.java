package checkoutflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmOrderWorkerTest {

    private ConfirmOrderWorker worker;

    @BeforeEach
    void setUp() {
        ConfirmOrderWorker.resetState();
        worker = new ConfirmOrderWorker();
    }

    @Test
    void taskDefName() {
        assertEquals("chk_confirm_order", worker.getTaskDefName());
    }

    @Test
    void returnsOrderId() {
        Task task = taskWith(Map.of("cartId", "cart-1", "userId", "usr-1",
                "paymentId", "pay-1", "grandTotal", 100.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("orderId"));
        assertTrue(result.getOutputData().get("orderId").toString().startsWith("ORD-"));
    }

    @Test
    void confirmedIsTrue() {
        Task task = taskWith(Map.of("cartId", "cart-2", "userId", "usr-2",
                "paymentId", "pay-2", "grandTotal", 50.0));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void returnsConfirmedAt() {
        Task task = taskWith(Map.of("cartId", "cart-3", "userId", "usr-3",
                "paymentId", "pay-3", "grandTotal", 75.0));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("confirmedAt"));
    }

    @Test
    void orderIdIsUnique() {
        Task task1 = taskWith(Map.of("cartId", "c1", "userId", "u1",
                "paymentId", "p1", "grandTotal", 10.0));
        Task task2 = taskWith(Map.of("cartId", "c2", "userId", "u2",
                "paymentId", "p2", "grandTotal", 20.0));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertNotEquals(r1.getOutputData().get("orderId"), r2.getOutputData().get("orderId"));
    }

    @Test
    void failsWithoutPaymentId() {
        Task task = taskWith(new HashMap<>(Map.of("cartId", "c-nopay", "userId", "u1",
                "grandTotal", 100.0)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("confirmed"));
    }

    @Test
    void failsWithZeroTotal() {
        Task task = taskWith(Map.of("cartId", "c-zero", "userId", "u1",
                "paymentId", "p1", "grandTotal", 0.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @Test
    void duplicateCartReturnsSameOrder() {
        Task task1 = taskWith(Map.of("cartId", "cart-dup", "userId", "u1",
                "paymentId", "p1", "grandTotal", 100.0));
        Task task2 = taskWith(Map.of("cartId", "cart-dup", "userId", "u1",
                "paymentId", "p1", "grandTotal", 100.0));

        String id1 = worker.execute(task1).getOutputData().get("orderId").toString();
        TaskResult r2 = worker.execute(task2);
        assertEquals(id1, r2.getOutputData().get("orderId"));
        assertEquals(true, r2.getOutputData().get("duplicate"));
    }

    @Test
    void includesOrderSummary() {
        Task task = taskWith(Map.of("cartId", "cart-sum", "userId", "usr-sum",
                "paymentId", "pay-sum", "grandTotal", 200.0));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("orderSummary"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
