package eventcorrelation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * perform  receiving an order event for event correlation.
 * Returns deterministic, fixed order data.
 */
public class ReceiveOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ec_receive_order";
    }

    @Override
    public TaskResult execute(Task task) {
        String correlationId = (String) task.getInputData().get("correlationId");
        String eventType = (String) task.getInputData().get("eventType");

        System.out.println("  [ec_receive_order] Receiving order event for correlation: " + correlationId);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "order");
        event.put("orderId", "ORD-7712");
        event.put("customerId", "C-001");
        event.put("amount", 599.99);
        event.put("currency", "USD");
        event.put("timestamp", "2026-01-15T10:00:00Z");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("event", event);
        return result;
    }
}
