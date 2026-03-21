package reportgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Aggregates raw data into summary metrics for reporting.
 * Input: rawData (list of records), reportType
 * Output: aggregated (map of metrics), aggregationCount
 */
public class AggregateResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rg_aggregate_results";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> data =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("rawData", List.of());

        Map<String, Integer> byRegion = new LinkedHashMap<>();
        Map<String, Integer> byProduct = new LinkedHashMap<>();
        int totalRevenue = 0;
        int totalUnits = 0;

        for (Map<String, Object> row : data) {
            int revenue = ((Number) row.getOrDefault("revenue", 0)).intValue();
            int units = ((Number) row.getOrDefault("units", 0)).intValue();
            String region = (String) row.getOrDefault("region", "Unknown");
            String product = (String) row.getOrDefault("product", "Unknown");

            totalRevenue += revenue;
            totalUnits += units;
            byRegion.merge(region, revenue, Integer::sum);
            byProduct.merge(product, revenue, Integer::sum);
        }

        String topRegion = byRegion.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");
        String topProduct = byProduct.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");

        Map<String, Object> aggregated = new LinkedHashMap<>();
        aggregated.put("totalRevenue", "$" + String.format("%,d", totalRevenue));
        aggregated.put("totalUnits", totalUnits);
        aggregated.put("avgRevenuePerRecord", data.isEmpty() ? "$0" : "$" + String.format("%.2f", (double) totalRevenue / data.size()));
        aggregated.put("byRegion", byRegion);
        aggregated.put("byProduct", byProduct);
        aggregated.put("topRegion", topRegion);
        aggregated.put("topProduct", topProduct);

        System.out.println("  [aggregate] Total revenue: " + aggregated.get("totalRevenue")
                + ", top region: " + topRegion + ", top product: " + topProduct);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregated", aggregated);
        result.getOutputData().put("aggregationCount", aggregated.size());
        return result;
    }
}
