package workflowinputoutput.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LookupPriceWorkerTest {

    private final LookupPriceWorker worker = new LookupPriceWorker();

    @Test
    void taskDefName() {
        assertEquals("lookup_price", worker.getTaskDefName());
    }

    @Test
    void looksUpWirelessKeyboard() {
        Task task = taskWith(Map.of("productId", "PROD-001", "quantity", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Wireless Keyboard", result.getOutputData().get("productName"));
        assertEquals(79.99, (double) result.getOutputData().get("unitPrice"), 0.001);
        assertEquals(3, result.getOutputData().get("quantity"));
        assertEquals(239.97, (double) result.getOutputData().get("subtotal"), 0.001);
    }

    @Test
    void looksUpMonitor() {
        Task task = taskWith(Map.of("productId", "PROD-002", "quantity", 2));
        TaskResult result = worker.execute(task);

        assertEquals("27\" Monitor", result.getOutputData().get("productName"));
        assertEquals(349.99, (double) result.getOutputData().get("unitPrice"), 0.001);
        assertEquals(699.98, (double) result.getOutputData().get("subtotal"), 0.001);
    }

    @Test
    void looksUpUsbcHub() {
        Task task = taskWith(Map.of("productId", "PROD-003", "quantity", 1));
        TaskResult result = worker.execute(task);

        assertEquals("USB-C Hub", result.getOutputData().get("productName"));
        assertEquals(49.99, (double) result.getOutputData().get("unitPrice"), 0.001);
        assertEquals(49.99, (double) result.getOutputData().get("subtotal"), 0.001);
    }

    @Test
    void unknownProductDefaultsToZero() {
        Task task = taskWith(Map.of("productId", "UNKNOWN-999", "quantity", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Unknown Product", result.getOutputData().get("productName"));
        assertEquals(0.0, (double) result.getOutputData().get("unitPrice"), 0.001);
        assertEquals(0.0, (double) result.getOutputData().get("subtotal"), 0.001);
    }

    @Test
    void defaultsQuantityToOneWhenMissing() {
        Task task = taskWith(Map.of("productId", "PROD-001"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("quantity"));
        assertEquals(79.99, (double) result.getOutputData().get("subtotal"), 0.001);
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("productId", "PROD-001", "quantity", 1));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("productName"));
        assertNotNull(result.getOutputData().get("unitPrice"));
        assertNotNull(result.getOutputData().get("quantity"));
        assertNotNull(result.getOutputData().get("subtotal"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
