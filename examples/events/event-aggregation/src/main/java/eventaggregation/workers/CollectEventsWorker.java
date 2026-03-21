package eventaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Collects events from a time window. Returns a fixed set of 6 transaction
 * events (purchases and refunds) along with the total event count.
 */
public class CollectEventsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eg_collect_events";
    }

    @Override
    public TaskResult execute(Task task) {
        String windowId = (String) task.getInputData().get("windowId");
        if (windowId == null || windowId.isBlank()) {
            windowId = "win-default";
        }

        Object windowDurationObj = task.getInputData().get("windowDurationSec");
        int windowDurationSec = 60;
        if (windowDurationObj instanceof Number) {
            windowDurationSec = ((Number) windowDurationObj).intValue();
        } else if (windowDurationObj instanceof String s && !s.isBlank()) {
            windowDurationSec = Integer.parseInt(s);
        }

        String eventSource = (String) task.getInputData().get("eventSource");
        if (eventSource == null || eventSource.isBlank()) {
            eventSource = "default-stream";
        }

        System.out.println("  [eg_collect_events] Collecting events from window: " + windowId
                + " (duration=" + windowDurationSec + "s, source=" + eventSource + ")");

        List<Map<String, Object>> events = List.of(
                Map.of("id", "e1", "type", "purchase", "amount", 49.99, "product", "Widget A"),
                Map.of("id", "e2", "type", "purchase", "amount", 129.00, "product", "Widget B"),
                Map.of("id", "e3", "type", "refund", "amount", -25.00, "product", "Widget A"),
                Map.of("id", "e4", "type", "purchase", "amount", 89.50, "product", "Widget C"),
                Map.of("id", "e5", "type", "purchase", "amount", 199.99, "product", "Widget B"),
                Map.of("id", "e6", "type", "purchase", "amount", 34.99, "product", "Widget A")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("events", events);
        result.getOutputData().put("eventCount", 6);
        return result;
    }
}
