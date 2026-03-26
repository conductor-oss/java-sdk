package exclusivejoin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VendorBWorkerTest {

    private final VendorBWorker worker = new VendorBWorker();

    @Test
    void taskDefName() {
        assertEquals("ej_vendor_b", worker.getTaskDefName());
    }

    @Test
    void returnsVendorBResult() {
        Task task = taskWith(Map.of("query", "wireless-keyboard"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> vendorResult = (Map<String, Object>) result.getOutputData().get("vendorResult");
        assertNotNull(vendorResult);
        assertEquals("B", vendorResult.get("vendor"));
        assertEquals(42.50, vendorResult.get("price"));
        assertEquals(450, vendorResult.get("responseTime"));
        assertEquals("wireless-keyboard", vendorResult.get("query"));
    }

    @Test
    void hasLowestPrice() {
        Task task = taskWith(Map.of("query", "test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> vendorResult = (Map<String, Object>) result.getOutputData().get("vendorResult");
        double price = ((Number) vendorResult.get("price")).doubleValue();
        assertTrue(price < 50.0, "Vendor B should have the lowest price");
    }

    @Test
    void returnsDeterministicValues() {
        Task task1 = taskWith(Map.of("query", "mouse"));
        Task task2 = taskWith(Map.of("query", "mouse"));
        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("vendorResult"),
                result2.getOutputData().get("vendorResult"));
    }

    @Test
    void defaultsQueryWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> vendorResult = (Map<String, Object>) result.getOutputData().get("vendorResult");
        assertEquals("unknown-product", vendorResult.get("query"));
    }

    @Test
    void defaultsQueryWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("query", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> vendorResult = (Map<String, Object>) result.getOutputData().get("vendorResult");
        assertEquals("unknown-product", vendorResult.get("query"));
    }

    @Test
    void resultContainsAllExpectedFields() {
        Task task = taskWith(Map.of("query", "wireless-keyboard"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> vendorResult = (Map<String, Object>) result.getOutputData().get("vendorResult");
        assertEquals(4, vendorResult.size());
        assertTrue(vendorResult.containsKey("vendor"));
        assertTrue(vendorResult.containsKey("price"));
        assertTrue(vendorResult.containsKey("responseTime"));
        assertTrue(vendorResult.containsKey("query"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
