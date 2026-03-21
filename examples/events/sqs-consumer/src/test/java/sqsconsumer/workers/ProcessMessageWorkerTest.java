package sqsconsumer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessMessageWorkerTest {

    private final ProcessMessageWorker worker = new ProcessMessageWorker();

    @Test
    void taskDefName() {
        assertEquals("qs_process_message", worker.getTaskDefName());
    }

    @Test
    void processesInvoiceGeneratedMessage() {
        Task task = taskWith(Map.of(
                "validatedData", Map.of(
                        "eventType", "invoice.generated",
                        "invoiceId", "INV-2026-0831",
                        "customerId", "C-2244",
                        "amount", 4500.00),
                "messageType", "invoice.generated"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("invoice_recorded", result.getOutputData().get("result"));
        assertEquals("INV-2026-0831", result.getOutputData().get("invoiceId"));
        assertEquals(38, result.getOutputData().get("processingTimeMs"));
    }

    @Test
    void logsEventForUnknownMessageType() {
        Task task = taskWith(Map.of(
                "validatedData", Map.of("eventType", "order.created"),
                "messageType", "order.created"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("event_logged", result.getOutputData().get("result"));
    }

    @Test
    void extractsInvoiceIdFromValidatedData() {
        Task task = taskWith(Map.of(
                "validatedData", Map.of("invoiceId", "INV-9999"),
                "messageType", "invoice.generated"));
        TaskResult result = worker.execute(task);

        assertEquals("INV-9999", result.getOutputData().get("invoiceId"));
    }

    @Test
    void handlesEmptyValidatedData() {
        Task task = taskWith(Map.of(
                "validatedData", Map.of(),
                "messageType", "invoice.generated"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("unknown", result.getOutputData().get("invoiceId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("event_logged", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("validatedData", null);
        input.put("messageType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("event_logged", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of(
                "validatedData", Map.of("invoiceId", "INV-001"),
                "messageType", "invoice.generated"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("processed"));
        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("invoiceId"));
        assertTrue(result.getOutputData().containsKey("processingTimeMs"));
    }

    @Test
    void processingTimeMsIsAlways38() {
        Task task = taskWith(Map.of(
                "validatedData", Map.of("invoiceId", "INV-001"),
                "messageType", "invoice.generated"));
        TaskResult result = worker.execute(task);

        assertEquals(38, result.getOutputData().get("processingTimeMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
