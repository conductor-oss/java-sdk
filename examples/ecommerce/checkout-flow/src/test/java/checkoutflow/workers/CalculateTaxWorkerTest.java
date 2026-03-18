package checkoutflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculateTaxWorkerTest {

    private final CalculateTaxWorker worker = new CalculateTaxWorker();

    @Test
    void taskDefName() {
        assertEquals("chk_calculate_tax", worker.getTaskDefName());
    }

    @Test
    void calculatesCaliforniaTax() {
        Task task = taskWith(Map.of("subtotal", 100.0, "shippingAddress", Map.of("state", "CA")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0875, ((Number) result.getOutputData().get("taxRate")).doubleValue());
        assertEquals(8.75, ((Number) result.getOutputData().get("tax")).doubleValue());
    }

    @Test
    void calculatesTexasTax() {
        Task task = taskWith(Map.of("subtotal", 100.0, "shippingAddress", Map.of("state", "TX")));
        TaskResult result = worker.execute(task);

        assertEquals(0.0819, ((Number) result.getOutputData().get("taxRate")).doubleValue());
        assertEquals(8.19, ((Number) result.getOutputData().get("tax")).doubleValue());
    }

    @Test
    void oregonNoSalesTax() {
        Task task = taskWith(Map.of("subtotal", 100.0, "shippingAddress", Map.of("state", "OR")));
        TaskResult result = worker.execute(task);

        assertEquals(0.0, ((Number) result.getOutputData().get("taxRate")).doubleValue());
        assertEquals(0.0, ((Number) result.getOutputData().get("tax")).doubleValue());
        assertEquals("exempt", result.getOutputData().get("taxType"));
    }

    @Test
    void newYorkTax() {
        Task task = taskWith(Map.of("subtotal", 200.0, "shippingAddress", Map.of("state", "NY")));
        TaskResult result = worker.execute(task);

        assertEquals(0.085, ((Number) result.getOutputData().get("taxRate")).doubleValue());
        assertEquals(17.0, ((Number) result.getOutputData().get("tax")).doubleValue());
    }

    @Test
    void ukVat() {
        Task task = taskWith(Map.of("subtotal", 100.0,
                "shippingAddress", Map.of("country", "GB")));
        TaskResult result = worker.execute(task);

        assertEquals(0.20, ((Number) result.getOutputData().get("taxRate")).doubleValue());
        assertEquals("vat", result.getOutputData().get("taxType"));
    }

    @Test
    void freeShippingOver100() {
        Task task = taskWith(Map.of("subtotal", 150.0, "shippingAddress", Map.of("state", "CA")));
        TaskResult result = worker.execute(task);

        assertEquals(0.0, ((Number) result.getOutputData().get("shipping")).doubleValue());
        assertEquals(true, result.getOutputData().get("freeShipping"));
    }

    @Test
    void standardShippingUnder100() {
        Task task = taskWith(Map.of("subtotal", 50.0, "shippingAddress", Map.of("state", "CA")));
        TaskResult result = worker.execute(task);

        assertEquals(9.99, ((Number) result.getOutputData().get("shipping")).doubleValue());
    }

    @Test
    void calculatesGrandTotal() {
        Task task = taskWith(Map.of("subtotal", 100.0, "shippingAddress", Map.of("state", "CA")));
        TaskResult result = worker.execute(task);

        // $100 + $8.75 tax + $0 shipping (free over $100) = $108.75
        double grandTotal = ((Number) result.getOutputData().get("grandTotal")).doubleValue();
        assertEquals(108.75, grandTotal, 0.01);
    }

    @Test
    void grandTotalWithShipping() {
        Task task = taskWith(Map.of("subtotal", 50.0, "shippingAddress", Map.of("state", "CA")));
        TaskResult result = worker.execute(task);

        // $50 + $4.375 tax + $9.99 shipping = ~$64.37
        double grandTotal = ((Number) result.getOutputData().get("grandTotal")).doubleValue();
        assertEquals(64.37, grandTotal, 0.01);
    }

    @Test
    void handlesStringSubtotal() {
        Task task = taskWith(new HashMap<>(Map.of("subtotal", "200.0", "shippingAddress", Map.of("state", "CA"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(((Number) result.getOutputData().get("grandTotal")).doubleValue() > 0);
    }

    @Test
    void handlesZeroSubtotal() {
        Task task = taskWith(Map.of("subtotal", 0, "shippingAddress", Map.of("state", "CA")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // $0 + $0 tax + $9.99 shipping
        assertEquals(9.99, ((Number) result.getOutputData().get("grandTotal")).doubleValue(), 0.01);
    }

    @Test
    void includesTaxBreakdown() {
        Task task = taskWith(Map.of("subtotal", 100.0, "shippingAddress", Map.of("state", "CA")));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("taxBreakdown"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
