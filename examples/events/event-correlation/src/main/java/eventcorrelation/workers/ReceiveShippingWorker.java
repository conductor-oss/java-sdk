package eventcorrelation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * perform  receiving a shipping event for event correlation.
 * Returns deterministic, fixed shipping data.
 */
public class ReceiveShippingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ec_receive_shipping";
    }

    @Override
    public TaskResult execute(Task task) {
        String correlationId = (String) task.getInputData().get("correlationId");
        String eventType = (String) task.getInputData().get("eventType");

        System.out.println("  [ec_receive_shipping] Receiving shipping event for correlation: " + correlationId);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "shipping");
        event.put("shipmentId", "SHP-5501");
        event.put("orderId", "ORD-7712");
        event.put("carrier", "FedEx");
        event.put("trackingNumber", "FX-998877");
        event.put("timestamp", "2026-01-15T10:02:00Z");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("event", event);
        return result;
    }
}
