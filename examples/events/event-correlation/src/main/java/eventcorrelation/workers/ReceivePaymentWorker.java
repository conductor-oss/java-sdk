package eventcorrelation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * perform  receiving a payment event for event correlation.
 * Returns deterministic, fixed payment data.
 */
public class ReceivePaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ec_receive_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        String correlationId = (String) task.getInputData().get("correlationId");
        String eventType = (String) task.getInputData().get("eventType");

        System.out.println("  [ec_receive_payment] Receiving payment event for correlation: " + correlationId);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "payment");
        event.put("paymentId", "PAY-3301");
        event.put("orderId", "ORD-7712");
        event.put("amount", 599.99);
        event.put("method", "credit_card");
        event.put("timestamp", "2026-01-15T10:01:00Z");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("event", event);
        return result;
    }
}
