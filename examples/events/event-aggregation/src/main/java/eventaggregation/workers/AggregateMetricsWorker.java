package eventaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Aggregates metrics from collected events. Produces fixed totals:
 * totalRevenue=478.47, avgOrderValue=100.70, purchaseCount=5, refundCount=1,
 * and a product breakdown.
 */
public class AggregateMetricsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eg_aggregate_metrics";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> events =
                (List<Map<String, Object>>) task.getInputData().get("events");
        if (events == null) {
            events = List.of();
        }

        Object eventCountObj = task.getInputData().get("eventCount");
        int eventCount = 0;
        if (eventCountObj instanceof Number) {
            eventCount = ((Number) eventCountObj).intValue();
        } else if (eventCountObj instanceof String s && !s.isBlank()) {
            eventCount = Integer.parseInt(s);
        }

        System.out.println("  [eg_aggregate_metrics] Aggregating " + eventCount + " events");

        Map<String, Object> aggregation = Map.of(
                "totalEvents", 6,
                "purchaseCount", 5,
                "refundCount", 1,
                "totalRevenue", 478.47,
                "avgOrderValue", 100.70,
                "productBreakdown", Map.of(
                        "Widget A", 3,
                        "Widget B", 2,
                        "Widget C", 1
                )
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregation", aggregation);
        return result;
    }
}
