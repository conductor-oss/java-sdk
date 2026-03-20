package sqsconsumer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateMessageWorkerTest {

    private final ValidateMessageWorker worker = new ValidateMessageWorker();

    @Test
    void taskDefName() {
        assertEquals("qs_validate_message", worker.getTaskDefName());
    }

    @Test
    void validatesCompleteMessage() {
        Task task = taskWith(Map.of(
                "parsedBody", Map.of(
                        "eventType", "invoice.generated",
                        "invoiceId", "INV-2026-0831",
                        "customerId", "C-2244",
                        "amount", 4500.00,
                        "dueDate", "2026-04-07"),
                "attributes", Map.of(
                        "ApproximateReceiveCount", "1",
                        "SentTimestamp", "1710900000000")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertEquals("invoice.generated", result.getOutputData().get("messageType"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void checksAllPassForValidMessage() {
        Task task = taskWith(Map.of(
                "parsedBody", Map.of(
                        "eventType", "invoice.generated",
                        "invoiceId", "INV-2026-0831",
                        "customerId", "C-2244",
                        "amount", 4500.00),
                "attributes", Map.of("ApproximateReceiveCount", "1")));
        TaskResult result = worker.execute(task);

        Map<String, Boolean> checks = (Map<String, Boolean>) result.getOutputData().get("checks");
        assertTrue(checks.get("hasEventType"));
        assertTrue(checks.get("hasInvoiceId"));
        assertTrue(checks.get("hasCustomerId"));
        assertTrue(checks.get("validAmount"));
        assertTrue(checks.get("firstReceive"));
    }

    @Test
    void invalidWhenMissingEventType() {
        Task task = taskWith(Map.of(
                "parsedBody", Map.of(
                        "invoiceId", "INV-001",
                        "customerId", "C-001",
                        "amount", 100.00),
                "attributes", Map.of("ApproximateReceiveCount", "1")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void invalidWhenAmountIsZero() {
        Task task = taskWith(Map.of(
                "parsedBody", Map.of(
                        "eventType", "invoice.generated",
                        "invoiceId", "INV-001",
                        "customerId", "C-001",
                        "amount", 0.0),
                "attributes", Map.of("ApproximateReceiveCount", "1")));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void invalidWhenNotFirstReceive() {
        Task task = taskWith(Map.of(
                "parsedBody", Map.of(
                        "eventType", "invoice.generated",
                        "invoiceId", "INV-001",
                        "customerId", "C-001",
                        "amount", 100.00),
                "attributes", Map.of("ApproximateReceiveCount", "3")));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void returnsValidatedDataFromParsedBody() {
        Map<String, Object> parsedBody = Map.of(
                "eventType", "invoice.generated",
                "invoiceId", "INV-2026-0831",
                "customerId", "C-2244",
                "amount", 4500.00);
        Task task = taskWith(Map.of(
                "parsedBody", parsedBody,
                "attributes", Map.of("ApproximateReceiveCount", "1")));
        TaskResult result = worker.execute(task);

        assertEquals(parsedBody, result.getOutputData().get("validatedData"));
    }

    @Test
    void handlesMissingParsedBody() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("valid"));
        assertEquals("unknown", result.getOutputData().get("messageType"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("parsedBody", null);
        input.put("attributes", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of(
                "parsedBody", Map.of(
                        "eventType", "invoice.generated",
                        "invoiceId", "INV-001",
                        "customerId", "C-001",
                        "amount", 100.00),
                "attributes", Map.of("ApproximateReceiveCount", "1")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("valid"));
        assertTrue(result.getOutputData().containsKey("checks"));
        assertTrue(result.getOutputData().containsKey("validatedData"));
        assertTrue(result.getOutputData().containsKey("messageType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
