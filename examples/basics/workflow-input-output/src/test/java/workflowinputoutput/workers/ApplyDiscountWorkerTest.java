package workflowinputoutput.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplyDiscountWorkerTest {

    private final ApplyDiscountWorker worker = new ApplyDiscountWorker();

    @Test
    void taskDefName() {
        assertEquals("apply_discount", worker.getTaskDefName());
    }

    @Test
    void appliesSave10Coupon() {
        Task task = taskWith(Map.of("subtotal", 100.0, "couponCode", "SAVE10"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.10, (double) result.getOutputData().get("discountRate"), 0.001);
        assertEquals(10.0, (double) result.getOutputData().get("discountAmount"), 0.001);
        assertEquals(90.0, (double) result.getOutputData().get("discountedTotal"), 0.001);
    }

    @Test
    void appliesSave20Coupon() {
        Task task = taskWith(Map.of("subtotal", 699.98, "couponCode", "SAVE20"));
        TaskResult result = worker.execute(task);

        assertEquals(0.20, (double) result.getOutputData().get("discountRate"), 0.001);
        assertEquals(139.996, (double) result.getOutputData().get("discountAmount"), 0.001);
        assertEquals(559.984, (double) result.getOutputData().get("discountedTotal"), 0.001);
    }

    @Test
    void appliesHalfCoupon() {
        Task task = taskWith(Map.of("subtotal", 200.0, "couponCode", "HALF"));
        TaskResult result = worker.execute(task);

        assertEquals(0.50, (double) result.getOutputData().get("discountRate"), 0.001);
        assertEquals(100.0, (double) result.getOutputData().get("discountAmount"), 0.001);
        assertEquals(100.0, (double) result.getOutputData().get("discountedTotal"), 0.001);
    }

    @Test
    void noDiscountWhenCouponMissing() {
        Task task = taskWith(new HashMap<>(Map.of("subtotal", 100.0)));
        task.getInputData().put("couponCode", null);
        TaskResult result = worker.execute(task);

        assertEquals(0.0, (double) result.getOutputData().get("discountRate"), 0.001);
        assertEquals(0.0, (double) result.getOutputData().get("discountAmount"), 0.001);
        assertEquals(100.0, (double) result.getOutputData().get("discountedTotal"), 0.001);
    }

    @Test
    void noDiscountWhenCouponBlank() {
        Task task = taskWith(Map.of("subtotal", 100.0, "couponCode", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(0.0, (double) result.getOutputData().get("discountRate"), 0.001);
        assertEquals(100.0, (double) result.getOutputData().get("discountedTotal"), 0.001);
    }

    @Test
    void noDiscountWhenCouponUnknown() {
        Task task = taskWith(Map.of("subtotal", 100.0, "couponCode", "INVALID"));
        TaskResult result = worker.execute(task);

        assertEquals(0.0, (double) result.getOutputData().get("discountRate"), 0.001);
        assertEquals(0.0, (double) result.getOutputData().get("discountAmount"), 0.001);
        assertEquals(100.0, (double) result.getOutputData().get("discountedTotal"), 0.001);
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("subtotal", 50.0, "couponCode", "SAVE10"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("couponCode"));
        assertNotNull(result.getOutputData().get("discountRate"));
        assertNotNull(result.getOutputData().get("discountAmount"));
        assertNotNull(result.getOutputData().get("discountedTotal"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
