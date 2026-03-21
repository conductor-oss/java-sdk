package sqsconsumer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeleteMessageWorkerTest {

    private final DeleteMessageWorker worker = new DeleteMessageWorker();

    @Test
    void taskDefName() {
        assertEquals("qs_delete_message", worker.getTaskDefName());
    }

    @Test
    void deletesMessageSuccessfully() {
        Task task = taskWith(Map.of(
                "queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/invoices-queue",
                "receiptHandle", "AQEBwJnKyrHigUMZj6rYigCgxlaS3SLy0a...",
                "processingResult", "invoice_recorded"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deleted"));
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("deletedAt"));
    }

    @Test
    void outputContainsBothKeys() {
        Task task = taskWith(Map.of(
                "queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/q",
                "receiptHandle", "handle-123",
                "processingResult", "invoice_recorded"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("deleted"));
        assertTrue(result.getOutputData().containsKey("deletedAt"));
        assertEquals(2, result.getOutputData().size());
    }

    @Test
    void handlesEmptyQueueUrl() {
        Map<String, Object> input = new HashMap<>();
        input.put("queueUrl", "");
        input.put("receiptHandle", "handle-123");
        input.put("processingResult", "invoice_recorded");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deleted"));
    }

    @Test
    void handlesEmptyReceiptHandle() {
        Map<String, Object> input = new HashMap<>();
        input.put("queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/q");
        input.put("receiptHandle", "");
        input.put("processingResult", "invoice_recorded");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deleted"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deleted"));
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("deletedAt"));
    }

    @Test
    void handlesNullValues() {
        Map<String, Object> input = new HashMap<>();
        input.put("queueUrl", null);
        input.put("receiptHandle", null);
        input.put("processingResult", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deleted"));
    }

    @Test
    void deletedAtTimestampIsFixed() {
        Task task = taskWith(Map.of(
                "queueUrl", "https://sqs.example.com/queue",
                "receiptHandle", "h1",
                "processingResult", "event_logged"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("deletedAt"));
    }

    @Test
    void deletedIsAlwaysTrue() {
        Task task = taskWith(Map.of(
                "queueUrl", "https://sqs.example.com/queue",
                "receiptHandle", "h1",
                "processingResult", "event_logged"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("deleted"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
