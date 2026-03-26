package webhooktrigger.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformDataWorkerTest {

    private final TransformDataWorker worker = new TransformDataWorker();

    @Test
    void taskDefName() {
        assertEquals("wt_transform_data", worker.getTaskDefName());
    }

    @Test
    void transformsValidatedData() {
        Task task = taskWith(new HashMap<>(Map.of(
                "validatedData", Map.of(
                        "orderId", "ORD-2026-4821",
                        "customer", "acme-corp",
                        "amount", 1250.00,
                        "currency", "USD",
                        "items", 3),
                "targetFormat", "canonical")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("transformedData"));
    }

    @Test
    void transformedDataHasCorrectFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "validatedData", Map.of(
                        "orderId", "ORD-2026-4821",
                        "customer", "acme-corp",
                        "amount", 1250.00,
                        "currency", "USD",
                        "items", 3),
                "targetFormat", "canonical")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformedData");
        assertEquals("ORD-2026-4821", transformed.get("id"));
        assertEquals("acme-corp", transformed.get("account"));
        assertEquals(3, transformed.get("lineItemCount"));
        assertEquals("2026-01-15T10:00:00Z", transformed.get("normalizedAt"));
    }

    @Test
    void transformedDataHasTotalWithValueAndCurrency() {
        Task task = taskWith(new HashMap<>(Map.of(
                "validatedData", Map.of(
                        "orderId", "ORD-2026-4821",
                        "customer", "acme-corp",
                        "amount", 1250.00,
                        "currency", "USD",
                        "items", 3),
                "targetFormat", "canonical")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformedData");
        @SuppressWarnings("unchecked")
        Map<String, Object> total = (Map<String, Object>) transformed.get("total");
        assertNotNull(total);
        assertEquals(1250.00, total.get("value"));
        assertEquals("USD", total.get("currency"));
    }

    @Test
    void returnsOrdersDbDestination() {
        Task task = taskWith(new HashMap<>(Map.of(
                "validatedData", Map.of("orderId", "ORD-2026-4821", "customer", "acme-corp",
                        "amount", 1250.00, "currency", "USD", "items", 3),
                "targetFormat", "canonical")));
        TaskResult result = worker.execute(task);

        assertEquals("orders_db", result.getOutputData().get("destination"));
    }

    @Test
    void handlesNullValidatedData() {
        Map<String, Object> input = new HashMap<>();
        input.put("validatedData", null);
        input.put("targetFormat", "canonical");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("transformedData"));
        assertEquals("orders_db", result.getOutputData().get("destination"));
    }

    @Test
    void handlesNullTargetFormat() {
        Map<String, Object> input = new HashMap<>();
        input.put("validatedData", Map.of("orderId", "ORD-2026-4821", "customer", "acme-corp",
                "amount", 1250.00, "currency", "USD", "items", 3));
        input.put("targetFormat", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("transformedData"));
        assertEquals("orders_db", result.getOutputData().get("destination"));
    }

    @Test
    void returnsFixedNormalizedAt() {
        Task task = taskWith(new HashMap<>(Map.of(
                "validatedData", Map.of("orderId", "ORD-2026-4821", "customer", "acme-corp",
                        "amount", 1250.00, "currency", "USD", "items", 3),
                "targetFormat", "canonical")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> transformed = (Map<String, Object>) result.getOutputData().get("transformedData");
        assertEquals("2026-01-15T10:00:00Z", transformed.get("normalizedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
