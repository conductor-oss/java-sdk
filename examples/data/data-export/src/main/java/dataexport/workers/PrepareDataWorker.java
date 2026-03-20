package dataexport.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Prepares data for export by querying the data source.
 * Input: query
 * Output: data (list of records), headers, recordCount
 */
public class PrepareDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dx_prepare_data";
    }

    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> data = List.of(
                Map.of("id", 1, "name", "Widget A", "category", "Hardware", "price", 29.99, "stock", 150, "lastUpdated", "2024-03-01"),
                Map.of("id", 2, "name", "Gadget B", "category", "Electronics", "price", 149.99, "stock", 45, "lastUpdated", "2024-03-10"),
                Map.of("id", 3, "name", "Tool C", "category", "Hardware", "price", 59.99, "stock", 200, "lastUpdated", "2024-03-05"),
                Map.of("id", 4, "name", "Device D", "category", "Electronics", "price", 299.99, "stock", 30, "lastUpdated", "2024-03-12"),
                Map.of("id", 5, "name", "Part E", "category", "Components", "price", 9.99, "stock", 500, "lastUpdated", "2024-03-08")
        );
        List<String> headers = List.of("id", "name", "category", "price", "stock", "lastUpdated");

        System.out.println("  [prepare] Prepared " + data.size() + " records with " + headers.size() + " columns for export");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("data", data);
        result.getOutputData().put("headers", headers);
        result.getOutputData().put("recordCount", data.size());
        return result;
    }
}
