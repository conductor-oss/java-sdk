package idempotentworkers.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChargeWorkerTest {

    @Test
    void taskDefName() {
        ChargeWorker worker = new ChargeWorker();
        assertEquals("idem_charge", worker.getTaskDefName());
    }

    @Test
    void firstCallProcessesCharge() {
        ChargeWorker worker = new ChargeWorker();
        Task task = taskWith(Map.of("orderId", "ORD-100", "amount", 49.99));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("charged"));
        assertEquals("ORD-100", result.getOutputData().get("orderId"));
        assertEquals(49.99, result.getOutputData().get("amount"));
        assertEquals(false, result.getOutputData().get("duplicate"));
    }

    @Test
    void secondCallReturnsCachedResult() {
        ChargeWorker worker = new ChargeWorker();
        Task task1 = taskWith(Map.of("orderId", "ORD-200", "amount", 75.00));
        worker.execute(task1);

        // Second call with the same orderId
        Task task2 = taskWith(Map.of("orderId", "ORD-200", "amount", 75.00));
        TaskResult result = worker.execute(task2);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("charged"));
        assertEquals("ORD-200", result.getOutputData().get("orderId"));
        assertEquals(75.00, result.getOutputData().get("amount"));
        // The cached result has duplicate=false because that's what was originally stored
        assertEquals(false, result.getOutputData().get("duplicate"));
    }

    @Test
    void differentOrdersProcessedIndependently() {
        ChargeWorker worker = new ChargeWorker();

        Task task1 = taskWith(Map.of("orderId", "ORD-300", "amount", 10.00));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("orderId", "ORD-301", "amount", 20.00));
        TaskResult result2 = worker.execute(task2);

        assertEquals(TaskResult.Status.COMPLETED, result1.getStatus());
        assertEquals(TaskResult.Status.COMPLETED, result2.getStatus());
        assertEquals("ORD-300", result1.getOutputData().get("orderId"));
        assertEquals(10.00, result1.getOutputData().get("amount"));
        assertEquals("ORD-301", result2.getOutputData().get("orderId"));
        assertEquals(20.00, result2.getOutputData().get("amount"));
    }

    @Test
    void cachedResultPreservesOriginalAmount() {
        ChargeWorker worker = new ChargeWorker();

        Task task1 = taskWith(Map.of("orderId", "ORD-400", "amount", 50.00));
        worker.execute(task1);

        // Second call with different amount but same orderId returns cached original
        Task task2 = taskWith(Map.of("orderId", "ORD-400", "amount", 999.99));
        TaskResult result = worker.execute(task2);

        assertEquals(50.00, result.getOutputData().get("amount"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        ChargeWorker worker = new ChargeWorker();
        Task task = taskWith(Map.of("orderId", "ORD-500", "amount", 25.00));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("charged"));
        assertTrue(result.getOutputData().containsKey("orderId"));
        assertTrue(result.getOutputData().containsKey("amount"));
        assertTrue(result.getOutputData().containsKey("duplicate"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
