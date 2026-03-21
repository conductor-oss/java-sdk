package reportgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Queries raw data for report generation.
 * Input: reportType, dateRange
 * Output: data (list of records), recordCount
 */
public class QueryDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rg_query_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String reportType = (String) task.getInputData().getOrDefault("reportType", "sales");

        List<Map<String, Object>> data = List.of(
                Map.of("date", "2024-03-01", "region", "North", "product", "Widget", "revenue", 15000, "units", 150),
                Map.of("date", "2024-03-01", "region", "South", "product", "Widget", "revenue", 12000, "units", 120),
                Map.of("date", "2024-03-01", "region", "North", "product", "Gadget", "revenue", 28000, "units", 56),
                Map.of("date", "2024-03-02", "region", "South", "product", "Gadget", "revenue", 22000, "units", 44),
                Map.of("date", "2024-03-02", "region", "North", "product", "Widget", "revenue", 18000, "units", 180),
                Map.of("date", "2024-03-02", "region", "South", "product", "Widget", "revenue", 9500, "units", 95),
                Map.of("date", "2024-03-02", "region", "North", "product", "Gadget", "revenue", 50000, "units", 70)
        );

        System.out.println("  [query] Fetched " + data.size() + " " + reportType + " records for date range");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("data", data);
        result.getOutputData().put("recordCount", data.size());
        return result;
    }
}
