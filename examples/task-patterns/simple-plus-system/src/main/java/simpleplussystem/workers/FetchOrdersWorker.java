package simpleplussystem.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * SIMPLE worker that fetches order data for a given store.
 *
 * In a real system this would call a database or API.
 * Here it returns sample data so the INLINE stats task can process it.
 */
public class FetchOrdersWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fetch_orders";
    }

    @Override
    public TaskResult execute(Task task) {
        String storeId = (String) task.getInputData().get("storeId");
        String dateRange = (String) task.getInputData().get("dateRange");

        System.out.println("  [SIMPLE] Fetching orders for store " + storeId
                + " (" + (dateRange != null ? dateRange : "all") + ")");

        // deterministic order data
        List<Map<String, Object>> orders = List.of(
                Map.of("id", "O-1", "amount", 150, "items", 3),
                Map.of("id", "O-2", "amount", 89, "items", 1),
                Map.of("id", "O-3", "amount", 320, "items", 5),
                Map.of("id", "O-4", "amount", 45, "items", 2),
                Map.of("id", "O-5", "amount", 210, "items", 4)
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orders", orders);
        return result;
    }
}
