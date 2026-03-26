package sqsconsumer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Validates an SQS message payload by checking required fields (eventType,
 * invoiceId, customerId, amount) and confirming it is a first receive.
 */
public class ValidateMessageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qs_validate_message";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> parsedBody = (Map<String, Object>) task.getInputData().get("parsedBody");
        if (parsedBody == null) {
            parsedBody = Map.of();
        }

        Map<String, Object> attributes = (Map<String, Object>) task.getInputData().get("attributes");
        if (attributes == null) {
            attributes = Map.of();
        }

        System.out.println("  [qs_validate_message] Validating message...");

        boolean hasEventType = parsedBody.containsKey("eventType") && parsedBody.get("eventType") != null;
        boolean hasInvoiceId = parsedBody.containsKey("invoiceId") && parsedBody.get("invoiceId") != null;
        boolean hasCustomerId = parsedBody.containsKey("customerId") && parsedBody.get("customerId") != null;

        boolean validAmount = false;
        Object amountObj = parsedBody.get("amount");
        if (amountObj instanceof Number) {
            validAmount = ((Number) amountObj).doubleValue() > 0;
        }

        boolean firstReceive = "1".equals(String.valueOf(attributes.get("ApproximateReceiveCount")));

        boolean valid = hasEventType && hasInvoiceId && hasCustomerId && validAmount && firstReceive;

        Map<String, Boolean> checks = Map.of(
                "hasEventType", hasEventType,
                "hasInvoiceId", hasInvoiceId,
                "hasCustomerId", hasCustomerId,
                "validAmount", validAmount,
                "firstReceive", firstReceive
        );

        String messageType = hasEventType ? String.valueOf(parsedBody.get("eventType")) : "unknown";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", valid);
        result.getOutputData().put("checks", checks);
        result.getOutputData().put("validatedData", parsedBody);
        result.getOutputData().put("messageType", messageType);
        return result;
    }
}
