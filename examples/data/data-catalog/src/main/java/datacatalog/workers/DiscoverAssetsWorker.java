package datacatalog.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Discovers data assets across schemas.
 * Input: dataSource, scanDepth
 * Output: assets, assetCount
 */
public class DiscoverAssetsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cg_discover_assets";
    }

    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> assets = List.of(
                Map.of("name", "customers", "type", "table", "schema", "public",
                        "rowCount", 50000, "columns", List.of("id", "name", "email", "phone", "address")),
                Map.of("name", "orders", "type", "table", "schema", "public",
                        "rowCount", 200000, "columns", List.of("id", "customer_id", "total", "status", "created_at")),
                Map.of("name", "products", "type", "table", "schema", "public",
                        "rowCount", 5000, "columns", List.of("id", "name", "price", "category", "description")),
                Map.of("name", "user_sessions", "type", "table", "schema", "analytics",
                        "rowCount", 1000000, "columns", List.of("session_id", "user_id", "ip_address", "duration")),
                Map.of("name", "daily_revenue", "type", "view", "schema", "reporting",
                        "rowCount", 365, "columns", List.of("date", "revenue", "orders", "avg_order_value"))
        );

        System.out.println("  [discover] Found " + assets.size() + " data assets across 3 schemas");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assets", assets);
        result.getOutputData().put("assetCount", assets.size());
        return result;
    }
}
