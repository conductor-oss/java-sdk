package requestaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Merges results from multiple services into a single response.
 * Input: user, orders, recommendations
 * Output: merged
 */
public class MergeResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agg_merge_results";
    }

    @Override
    public TaskResult execute(Task task) {
        Object user = task.getInputData().get("user");
        Object orders = task.getInputData().get("orders");
        Object recommendations = task.getInputData().get("recommendations");

        System.out.println("  [merge] Combining results from 3 services");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("merged", Map.of(
                "user", user != null ? user : Map.of(),
                "orders", orders != null ? orders : Map.of(),
                "recommendations", recommendations != null ? recommendations : Map.of()));
        return result;
    }
}
