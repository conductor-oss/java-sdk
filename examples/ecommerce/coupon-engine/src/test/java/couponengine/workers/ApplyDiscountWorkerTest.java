package couponengine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ApplyDiscountWorkerTest {
    private final ApplyDiscountWorker worker = new ApplyDiscountWorker();
    @Test void taskDefName() { assertEquals("cpn_apply_discount", worker.getTaskDefName()); }
    @Test void appliesPercentageDiscount() {
        Task task = taskWith(Map.of("couponCode", "TEST", "cartTotal", 100.0, "discountType", "percentage", "discountValue", 20));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(20.0, ((Number) r.getOutputData().get("discountAmount")).doubleValue());
        assertEquals(80.0, ((Number) r.getOutputData().get("newTotal")).doubleValue());
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
