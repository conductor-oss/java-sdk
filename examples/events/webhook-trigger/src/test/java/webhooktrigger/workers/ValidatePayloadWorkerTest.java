package webhooktrigger.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidatePayloadWorkerTest {

    private final ValidatePayloadWorker worker = new ValidatePayloadWorker();

    @Test
    void taskDefName() {
        assertEquals("wt_validate_payload", worker.getTaskDefName());
    }

    @Test
    void validatesPayloadSuccessfully() {
        Task task = taskWith(new HashMap<>(Map.of(
                "eventType", "order.created",
                "parsedData", Map.of("orderId", "ORD-2026-4821", "customer", "acme-corp",
                        "amount", 1250.00, "currency", "USD", "items", 3),
                "schema", "order_v2")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void returnsChecks() {
        Task task = taskWith(new HashMap<>(Map.of(
                "eventType", "order.created",
                "parsedData", Map.of("orderId", "ORD-2026-4821", "customer", "acme-corp"),
                "schema", "order_v2")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Boolean> checks = (Map<String, Boolean>) result.getOutputData().get("checks");
        assertNotNull(checks);
        assertTrue(checks.get("hasOrderId"));
        assertTrue(checks.get("hasCustomer"));
        assertTrue(checks.get("validAmount"));
        assertTrue(checks.get("validCurrency"));
    }

    @Test
    void returnsValidatedDataMatchingInput() {
        Map<String, Object> parsedData = Map.of(
                "orderId", "ORD-2026-4821",
                "customer", "acme-corp",
                "amount", 1250.00,
                "currency", "USD",
                "items", 3);
        Task task = taskWith(new HashMap<>(Map.of(
                "eventType", "order.created",
                "parsedData", parsedData,
                "schema", "order_v2")));
        TaskResult result = worker.execute(task);

        assertEquals(parsedData, result.getOutputData().get("validatedData"));
    }

    @Test
    void returnsCanonicalTargetFormat() {
        Task task = taskWith(new HashMap<>(Map.of(
                "eventType", "order.created",
                "parsedData", Map.of("orderId", "ORD-2026-4821"),
                "schema", "order_v2")));
        TaskResult result = worker.execute(task);

        assertEquals("canonical", result.getOutputData().get("targetFormat"));
    }

    @Test
    void handlesNullParsedData() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", "order.created");
        input.put("parsedData", null);
        input.put("schema", "order_v2");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertNotNull(result.getOutputData().get("validatedData"));
    }

    @Test
    void handlesNullSchema() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", "order.created");
        input.put("parsedData", Map.of("orderId", "ORD-2026-4821"));
        input.put("schema", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertNotNull(result.getOutputData().get("checks"));
        assertNotNull(result.getOutputData().get("validatedData"));
        assertNotNull(result.getOutputData().get("targetFormat"));
    }

    @Test
    void handlesEmptyEventType() {
        Task task = taskWith(new HashMap<>(Map.of(
                "eventType", "",
                "parsedData", Map.of("orderId", "ORD-2026-4821"),
                "schema", "order_v2")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
