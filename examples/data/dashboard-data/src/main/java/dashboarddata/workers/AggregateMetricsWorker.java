package dashboarddata.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregates raw metrics for the dashboard.
 * Input: dashboardId, timeRange
 * Output: metrics (map of metric objects), metricCount
 */
public class AggregateMetricsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dh_aggregate_metrics";
    }

    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("activeUsers", Map.of("current", 1247, "previous", 1180, "trend", "up"));
        metrics.put("pageViews", Map.of("current", 45230, "previous", 42100, "trend", "up"));
        metrics.put("avgResponseTime", Map.of("current", 182, "previous", 195, "trend", "down", "unit", "ms"));
        metrics.put("errorRate", Map.of("current", 0.8, "previous", 1.2, "trend", "down", "unit", "%"));
        metrics.put("revenue", Map.of("current", 89500, "previous", 82000, "trend", "up", "unit", "$"));
        metrics.put("orders", Map.of("current", 342, "previous", 310, "trend", "up"));
        metrics.put("conversionRate", Map.of("current", 3.2, "previous", 2.9, "trend", "up", "unit", "%"));
        metrics.put("bounceRate", Map.of("current", 38.5, "previous", 41.2, "trend", "down", "unit", "%"));

        System.out.println("  [aggregate] Aggregated " + metrics.size() + " metrics for dashboard");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metrics", metrics);
        result.getOutputData().put("metricCount", metrics.size());
        return result;
    }
}
