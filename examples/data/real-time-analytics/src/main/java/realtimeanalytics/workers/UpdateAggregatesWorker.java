package realtimeanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Updates running aggregates with the latest window metrics.
 * Input: processed (list), windowMetrics (map)
 * Output: aggregates (map), updatedCount
 */
public class UpdateAggregatesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ry_update_aggregates";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> windowMetrics =
                (Map<String, Object>) task.getInputData().getOrDefault("windowMetrics", Map.of());

        Map<String, Object> aggregates = new LinkedHashMap<>();
        aggregates.put("totalEventsLast1h", 4520);
        aggregates.put("avgLatencyLast1h", "165ms");
        aggregates.put("errorRateLast1h", "2.1%");
        aggregates.put("activeUsersLast1h", 312);
        aggregates.put("currentWindow", windowMetrics);
        aggregates.put("p95Latency", "450ms");
        aggregates.put("p99Latency", "820ms");

        System.out.println("  [update] Updated aggregates: " + aggregates.size()
                + " metrics, error rate: " + aggregates.get("errorRateLast1h"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregates", aggregates);
        result.getOutputData().put("updatedCount", aggregates.size());
        return result;
    }
}
