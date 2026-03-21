package sqsconsumer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveMessageWorkerTest {

    private final ReceiveMessageWorker worker = new ReceiveMessageWorker();

    @Test
    void taskDefName() {
        assertEquals("qs_receive_message", worker.getTaskDefName());
    }

    @Test
    void receivesMessageWithAllInputs() {
        Task task = taskWith(Map.of(
                "queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/invoices-queue",
                "messageId", "msg-fixed-001",
                "receiptHandle", "AQEBwJnKyrHigUMZj6rYigCgxlaS3SLy0a...",
                "body", "{\"eventType\":\"invoice.generated\"}"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("parsedBody"));
        assertNotNull(result.getOutputData().get("attributes"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void parsedBodyContainsExpectedFields() {
        Task task = taskWith(Map.of(
                "queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/invoices-queue",
                "messageId", "msg-fixed-001",
                "receiptHandle", "handle-123",
                "body", "{}"));
        TaskResult result = worker.execute(task);

        Map<String, Object> parsedBody = (Map<String, Object>) result.getOutputData().get("parsedBody");
        assertEquals("invoice.generated", parsedBody.get("eventType"));
        assertEquals("INV-2026-0831", parsedBody.get("invoiceId"));
        assertEquals("C-2244", parsedBody.get("customerId"));
        assertEquals(4500.00, parsedBody.get("amount"));
        assertEquals("2026-04-07", parsedBody.get("dueDate"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void attributesContainSqsMetadata() {
        Task task = taskWith(Map.of(
                "queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/invoices-queue",
                "messageId", "msg-fixed-001",
                "receiptHandle", "handle-123",
                "body", "{}"));
        TaskResult result = worker.execute(task);

        Map<String, String> attributes = (Map<String, String>) result.getOutputData().get("attributes");
        assertEquals("1", attributes.get("ApproximateReceiveCount"));
        assertEquals("1710900000000", attributes.get("SentTimestamp"));
        assertEquals("AIDAEXAMPLE", attributes.get("SenderId"));
        assertEquals("1710900000000", attributes.get("ApproximateFirstReceiveTimestamp"));
    }

    @Test
    void handlesEmptyBody() {
        Map<String, Object> input = new HashMap<>();
        input.put("queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/invoices-queue");
        input.put("messageId", "msg-fixed-001");
        input.put("receiptHandle", "handle-123");
        input.put("body", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("parsedBody"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("parsedBody"));
        assertNotNull(result.getOutputData().get("attributes"));
    }

    @Test
    void handlesNullValues() {
        Map<String, Object> input = new HashMap<>();
        input.put("queueUrl", null);
        input.put("messageId", null);
        input.put("receiptHandle", null);
        input.put("body", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("parsedBody"));
    }

    @Test
    void outputContainsBothKeys() {
        Task task = taskWith(Map.of(
                "queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/q",
                "messageId", "m1",
                "receiptHandle", "r1",
                "body", "{}"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("parsedBody"));
        assertTrue(result.getOutputData().containsKey("attributes"));
        assertEquals(2, result.getOutputData().size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
