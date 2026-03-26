package requestaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Generates recommendations for a user.
 * Input: userId
 * Output: items, count
 */
public class FetchRecommendationsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agg_fetch_recommendations";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        if (userId == null) userId = "unknown";

        System.out.println("  [recs] Generating recommendations for " + userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("items", List.of("Widget Pro", "Gadget X"));
        result.getOutputData().put("count", 2);
        return result;
    }
}
