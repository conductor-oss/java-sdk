package dashboarddata.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Computes KPIs from aggregated metrics.
 * Input: metrics (map of metric objects)
 * Output: kpis (list of KPI objects), kpiCount
 */
public class ComputeKpisWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dh_compute_kpis";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> metrics = (Map<String, Object>) task.getInputData().getOrDefault("metrics", Map.of());

        List<Map<String, Object>> kpis = new ArrayList<>();

        Map<String, Object> activeUsers = (Map<String, Object>) metrics.getOrDefault("activeUsers", Map.of());
        double auCurrent = toDouble(activeUsers.get("current"));
        double auPrevious = toDouble(activeUsers.get("previous"));
        if (auPrevious > 0) {
            String growth = String.format("%.1f%%", ((auCurrent - auPrevious) / auPrevious) * 100);
            kpis.add(Map.of("name", "User Growth", "value", growth, "status", "green"));
        }

        Map<String, Object> revenue = (Map<String, Object>) metrics.getOrDefault("revenue", Map.of());
        double revCurrent = toDouble(revenue.get("current"));
        double revPrevious = toDouble(revenue.get("previous"));
        if (revPrevious > 0) {
            String revGrowth = String.format("%.1f%%", ((revCurrent - revPrevious) / revPrevious) * 100);
            kpis.add(Map.of("name", "Revenue Growth", "value", revGrowth, "status", "green"));
        }

        Map<String, Object> errorRate = (Map<String, Object>) metrics.getOrDefault("errorRate", Map.of());
        double errCurrent = toDouble(errorRate.get("current"));
        kpis.add(Map.of("name", "Error Rate", "value", errCurrent + "%",
                "status", errCurrent < 1 ? "green" : "red"));

        Map<String, Object> avgResp = (Map<String, Object>) metrics.getOrDefault("avgResponseTime", Map.of());
        double respCurrent = toDouble(avgResp.get("current"));
        kpis.add(Map.of("name", "Avg Response Time", "value", (int) respCurrent + "ms",
                "status", respCurrent < 200 ? "green" : "yellow"));

        Map<String, Object> convRate = (Map<String, Object>) metrics.getOrDefault("conversionRate", Map.of());
        double convCurrent = toDouble(convRate.get("current"));
        kpis.add(Map.of("name", "Conversion Rate", "value", convCurrent + "%", "status", "green"));

        long greenCount = kpis.stream().filter(k -> "green".equals(k.get("status"))).count();
        long nonGreenCount = kpis.size() - greenCount;
        System.out.println("  [kpis] Computed " + kpis.size() + " KPIs - " + greenCount + " green, " + nonGreenCount + " non-green");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("kpis", kpis);
        result.getOutputData().put("kpiCount", kpis.size());
        return result;
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        return 0.0;
    }
}
