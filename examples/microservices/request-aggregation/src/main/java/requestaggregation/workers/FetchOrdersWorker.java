package requestaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Fetches recent orders for a user.
 * Input: userId
 * Output: orders (list)
 */
public class FetchOrdersWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agg_fetch_orders";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        if (userId == null) userId = "unknown";

        System.out.println("  [orders] Fetching recent orders for " + userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orders", List.of(
                Map.of("id", "ORD-1", "total", 59.99),
                Map.of("id", "ORD-2", "total", 124.50)));
        return result;
    }
}
