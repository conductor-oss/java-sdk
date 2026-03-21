package eventcorrelation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Correlates order, payment, and shipping events into a unified data set.
 * Returns deterministic output with fixed matchScore of 1.0.
 */
public class CorrelateEventsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ec_correlate_events";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String correlationId = (String) task.getInputData().get("correlationId");
        Map<String, Object> orderEvent = (Map<String, Object>) task.getInputData().get("orderEvent");
        Map<String, Object> paymentEvent = (Map<String, Object>) task.getInputData().get("paymentEvent");
        Map<String, Object> shippingEvent = (Map<String, Object>) task.getInputData().get("shippingEvent");

        String orderId = orderEvent != null ? (String) orderEvent.get("orderId") : "UNKNOWN";
        String customerId = orderEvent != null ? (String) orderEvent.get("customerId") : "UNKNOWN";
        Object orderAmountObj = orderEvent != null ? orderEvent.get("amount") : 0.0;
        double orderAmount = orderAmountObj instanceof Number ? ((Number) orderAmountObj).doubleValue() : 0.0;

        Object paymentAmountObj = paymentEvent != null ? paymentEvent.get("amount") : 0.0;
        double paymentAmount = paymentAmountObj instanceof Number ? ((Number) paymentAmountObj).doubleValue() : 0.0;
        String paymentMethod = paymentEvent != null ? (String) paymentEvent.get("method") : "unknown";

        String carrier = shippingEvent != null ? (String) shippingEvent.get("carrier") : "unknown";
        String trackingNumber = shippingEvent != null ? (String) shippingEvent.get("trackingNumber") : "unknown";

        System.out.println("  [ec_correlate_events] Correlating events for: " + correlationId);
        System.out.println("    -> Order: " + orderId + " | Payment: " + paymentAmount + " | Carrier: " + carrier);

        Map<String, Object> correlatedData = new LinkedHashMap<>();
        correlatedData.put("orderId", orderId);
        correlatedData.put("customerId", customerId);
        correlatedData.put("orderAmount", orderAmount);
        correlatedData.put("paymentAmount", paymentAmount);
        correlatedData.put("paymentMethod", paymentMethod);
        correlatedData.put("carrier", carrier);
        correlatedData.put("trackingNumber", trackingNumber);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("eventsCorrelated", 3);
        result.getOutputData().put("matchScore", 1.0);
        result.getOutputData().put("correlatedData", correlatedData);
        return result;
    }
}
