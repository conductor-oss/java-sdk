package eventdrivenmicroservices.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Emits the payment.processed event.
 * Input: eventType, orderId, transactionId
 * Output: published (true), eventType
 */
public class EmitPaymentProcessedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dm_emit_payment_processed";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null) {
            eventType = "payment.processed";
        }

        String transactionId = task.getInputData().get("transactionId") != null
                ? String.valueOf(task.getInputData().get("transactionId"))
                : "unknown";

        System.out.println("  [event-bus] Published: " + eventType
                + " (txn: " + transactionId + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("published", true);
        result.getOutputData().put("eventType", eventType);
        return result;
    }
}
