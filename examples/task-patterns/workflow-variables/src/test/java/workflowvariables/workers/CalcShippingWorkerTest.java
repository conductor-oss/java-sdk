package workflowvariables.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalcShippingWorkerTest {

    private final CalcShippingWorker worker = new CalcShippingWorker();

    @Test
    void taskDefName() {
        assertEquals("wv_calc_shipping", worker.getTaskDefName());
    }

    @Test
    void freeShippingForGoldTier() {
        Task task = taskWith(Map.of(
                "afterDiscount", 50.0,
                "itemCount", 2,
                "tier", "gold"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, ((Number) result.getOutputData().get("shippingCost")).doubleValue(), 0.01);
        assertTrue((Boolean) result.getOutputData().get("freeShipping"));
        assertEquals(50.0, ((Number) result.getOutputData().get("finalTotal")).doubleValue(), 0.01);
    }

    @Test
    void freeShippingForOrdersOver100() {
        Task task = taskWith(Map.of(
                "afterDiscount", 150.0,
                "itemCount", 3,
                "tier", "bronze"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, ((Number) result.getOutputData().get("shippingCost")).doubleValue(), 0.01);
        assertTrue((Boolean) result.getOutputData().get("freeShipping"));
        assertEquals(150.0, ((Number) result.getOutputData().get("finalTotal")).doubleValue(), 0.01);
    }

    @Test
    void paidShippingForNonGoldUnder100() {
        Task task = taskWith(Map.of(
                "afterDiscount", 50.0,
                "itemCount", 3,
                "tier", "silver"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // 5.99 + (3-1) * 1.5 = 5.99 + 3.0 = 8.99
        assertEquals(8.99, ((Number) result.getOutputData().get("shippingCost")).doubleValue(), 0.01);
        assertFalse((Boolean) result.getOutputData().get("freeShipping"));
        assertEquals(58.99, ((Number) result.getOutputData().get("finalTotal")).doubleValue(), 0.01);
    }

    @Test
    void singleItemShipping() {
        Task task = taskWith(Map.of(
                "afterDiscount", 30.0,
                "itemCount", 1,
                "tier", "bronze"
        ));
        TaskResult result = worker.execute(task);

        // 5.99 + (1-1) * 1.5 = 5.99
        assertEquals(5.99, ((Number) result.getOutputData().get("shippingCost")).doubleValue(), 0.01);
        assertEquals(35.99, ((Number) result.getOutputData().get("finalTotal")).doubleValue(), 0.01);
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of(
                "afterDiscount", 80.0,
                "itemCount", 2,
                "tier", "silver"
        ));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("shippingCost"));
        assertNotNull(result.getOutputData().get("freeShipping"));
        assertNotNull(result.getOutputData().get("finalTotal"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
