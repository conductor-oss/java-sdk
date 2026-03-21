package sqsconsumer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes a validated SQS message based on its type. For invoice.generated
 * events, records the invoice and returns the processing result.
 */
public class ProcessMessageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qs_process_message";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> validatedData = (Map<String, Object>) task.getInputData().get("validatedData");
        if (validatedData == null) {
            validatedData = Map.of();
        }

        String messageType = (String) task.getInputData().get("messageType");
        if (messageType == null || messageType.isBlank()) {
            messageType = "unknown";
        }

        System.out.println("  [qs_process_message] Processing message type: " + messageType);

        String processingResult;
        if ("invoice.generated".equals(messageType)) {
            processingResult = "invoice_recorded";
        } else {
            processingResult = "event_logged";
        }

        String invoiceId = validatedData.containsKey("invoiceId")
                ? String.valueOf(validatedData.get("invoiceId"))
                : "unknown";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("result", processingResult);
        result.getOutputData().put("invoiceId", invoiceId);
        result.getOutputData().put("processingTimeMs", 38);
        return result;
    }
}
