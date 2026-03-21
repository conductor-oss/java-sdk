package passingoutput.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Enriches a report by computing growth rate and health insights.
 * Receives the entire metrics object, individual fields, and arrays from the previous task.
 */
public class EnrichReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "enrich_report";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> metrics = (Map<String, Object>) task.getInputData().get("metrics");
        List<Map<String, Object>> topProducts = (List<Map<String, Object>>) task.getInputData().get("topProducts");
        Object totalRevenueObj = task.getInputData().get("totalRevenue");

        System.out.println("  [enrich] Revenue: $" + totalRevenueObj
                + ", Products: " + (topProducts != null ? topProducts.size() : 0));

        int previousRevenue = 110000;
        int currentRevenue = ((Number) metrics.get("revenue")).intValue();
        double growthRate = Math.round(((double) (currentRevenue - previousRevenue) / previousRevenue) * 1000.0) / 10.0;

        double returnRate = ((Number) metrics.get("returnRate")).doubleValue();
        String healthScore = returnRate < 0.05 ? "HEALTHY" : "NEEDS ATTENTION";

        String topProduct = topProducts != null && !topProducts.isEmpty()
                ? (String) topProducts.get(0).get("name")
                : "N/A";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        result.getOutputData().put("insights", Map.of(
                "yoyGrowth", growthRate + "%",
                "topProduct", topProduct,
                "healthScore", healthScore
        ));
        result.getOutputData().put("growthRate", growthRate);
        result.getOutputData().put("enrichedAt", Instant.now().toString());

        return result;
    }
}
