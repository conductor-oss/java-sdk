package scheduledreports.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Queries data sources for a report.
 * Input: reportType, dateRange
 * Output: data, rowCount, queryTimeMs
 */
public class QueryDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sch_query_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String reportType = (String) task.getInputData().get("reportType");
        String dateRange = (String) task.getInputData().get("dateRange");
        if (reportType == null) reportType = "unknown";
        if (dateRange == null) dateRange = "unspecified";

        System.out.println("  [query] Querying data for " + reportType + " report (" + dateRange + ")...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("data", Map.of("revenue", 145000, "orders", 3200, "avgOrderValue", 45.31));
        result.getOutputData().put("rowCount", 3200);
        result.getOutputData().put("queryTimeMs", 850);
        return result;
    }
}
