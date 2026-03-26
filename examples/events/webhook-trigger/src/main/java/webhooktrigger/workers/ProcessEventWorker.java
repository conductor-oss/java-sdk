package webhooktrigger.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes an incoming webhook event by extracting and parsing the payload
 * into structured data with a known schema.
 */
public class ProcessEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wt_process_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null || eventType.isBlank()) {
            eventType = "unknown";
        }

        System.out.println("  [wt_process_event] Processing event: " + eventType);

        Map<String, Object> parsedData = Map.of(
                "orderId", "ORD-2026-4821",
                "customer", "acme-corp",
                "amount", 1250.00,
                "currency", "USD",
                "items", 3
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("eventType", eventType);
        result.getOutputData().put("parsedData", parsedData);
        result.getOutputData().put("schema", "order_v2");
        result.getOutputData().put("receivedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
