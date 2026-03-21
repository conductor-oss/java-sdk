package eventcorrelation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessCorrelatedWorkerTest {

    private final ProcessCorrelatedWorker worker = new ProcessCorrelatedWorker();

    @Test
    void taskDefName() {
        assertEquals("ec_process_correlated", worker.getTaskDefName());
    }

    @Test
    void returnsActionAndProcessedAt() {
        Map<String, Object> correlatedData = Map.of(
                "orderId", "ORD-7712", "customerId", "C-001",
                "orderAmount", 599.99, "paymentAmount", 599.99,
                "paymentMethod", "credit_card", "carrier", "FedEx",
                "trackingNumber", "FX-998877");

        Task task = taskWith(Map.of(
                "correlationId", "corr-fixed-001",
                "correlatedData", correlatedData,
                "matchScore", 1.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("fulfill_order", result.getOutputData().get("action"));
        assertEquals("2026-01-15T10:03:00Z", result.getOutputData().get("processedAt"));
    }

    @Test
    void summaryContainsOrderId() {
        Map<String, Object> correlatedData = Map.of(
                "orderId", "ORD-7712", "paymentMethod", "credit_card", "carrier", "FedEx");

        Task task = taskWith(Map.of(
                "correlationId", "corr-1",
                "correlatedData", correlatedData,
                "matchScore", 1.0));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("ORD-7712"));
    }

    @Test
    void summaryContainsPaymentMethod() {
        Map<String, Object> correlatedData = Map.of(
                "orderId", "ORD-7712", "paymentMethod", "credit_card", "carrier", "FedEx");

        Task task = taskWith(Map.of(
                "correlationId", "corr-1",
                "correlatedData", correlatedData,
                "matchScore", 1.0));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("credit_card"));
    }

    @Test
    void summaryContainsCarrier() {
        Map<String, Object> correlatedData = Map.of(
                "orderId", "ORD-7712", "paymentMethod", "credit_card", "carrier", "FedEx");

        Task task = taskWith(Map.of(
                "correlationId", "corr-1",
                "correlatedData", correlatedData,
                "matchScore", 1.0));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("FedEx"));
    }

    @Test
    void handlesNullCorrelatedData() {
        Map<String, Object> input = new HashMap<>();
        input.put("correlationId", "corr-1");
        input.put("correlatedData", null);
        input.put("matchScore", 1.0);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("fulfill_order", result.getOutputData().get("action"));

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("UNKNOWN"));
    }

    @Test
    void handlesNullMatchScore() {
        Map<String, Object> correlatedData = Map.of(
                "orderId", "ORD-7712", "paymentMethod", "credit_card", "carrier", "FedEx");

        Map<String, Object> input = new HashMap<>();
        input.put("correlationId", "corr-1");
        input.put("correlatedData", correlatedData);
        input.put("matchScore", null);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("fulfill_order", result.getOutputData().get("action"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Map<String, Object> correlatedData = Map.of(
                "orderId", "ORD-7712", "paymentMethod", "credit_card", "carrier", "FedEx");

        Task task = taskWith(Map.of(
                "correlationId", "corr-1",
                "correlatedData", correlatedData,
                "matchScore", 1.0));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("action"));
        assertTrue(result.getOutputData().containsKey("processedAt"));
        assertTrue(result.getOutputData().containsKey("summary"));
    }

    @Test
    void processedAtIsFixedTimestamp() {
        Map<String, Object> correlatedData = Map.of(
                "orderId", "ORD-001", "paymentMethod", "wire", "carrier", "UPS");

        Task task = taskWith(Map.of(
                "correlationId", "corr-2",
                "correlatedData", correlatedData,
                "matchScore", 0.9));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:03:00Z", result.getOutputData().get("processedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
