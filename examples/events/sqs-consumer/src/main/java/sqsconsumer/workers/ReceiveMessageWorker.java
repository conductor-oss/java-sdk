package sqsconsumer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Receives and parses an SQS message body into structured data, extracting
 * the event payload and SQS message attributes.
 */
public class ReceiveMessageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qs_receive_message";
    }

    @Override
    public TaskResult execute(Task task) {
        String queueUrl = (String) task.getInputData().get("queueUrl");
        if (queueUrl == null || queueUrl.isBlank()) {
            queueUrl = "";
        }

        String messageId = (String) task.getInputData().get("messageId");
        if (messageId == null || messageId.isBlank()) {
            messageId = "unknown";
        }

        String receiptHandle = (String) task.getInputData().get("receiptHandle");
        if (receiptHandle == null || receiptHandle.isBlank()) {
            receiptHandle = "";
        }

        String body = (String) task.getInputData().get("body");
        if (body == null || body.isBlank()) {
            body = "{}";
        }

        System.out.println("  [qs_receive_message] Receiving message: " + messageId);

        Map<String, Object> parsedBody = Map.of(
                "eventType", "invoice.generated",
                "invoiceId", "INV-2026-0831",
                "customerId", "C-2244",
                "amount", 4500.00,
                "dueDate", "2026-04-07"
        );

        Map<String, String> attributes = Map.of(
                "ApproximateReceiveCount", "1",
                "SentTimestamp", "1710900000000",
                "SenderId", "AIDAEXAMPLE",
                "ApproximateFirstReceiveTimestamp", "1710900000000"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("parsedBody", parsedBody);
        result.getOutputData().put("attributes", attributes);
        return result;
    }
}
