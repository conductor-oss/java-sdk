package recommendationengine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Gathers user browsing behavior: viewed products, purchased products,
 * browsing categories, and session count.
 */
public class CollectBehaviorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rec_collect_behavior";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        if (userId == null) userId = "unknown";

        String context = (String) task.getInputData().get("context");
        if (context == null) context = "homepage";

        System.out.println("  [collect] Gathering behavior for user " + userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("viewedProducts", List.of("SKU-101", "SKU-205", "SKU-312", "SKU-450"));
        result.getOutputData().put("purchasedProducts", List.of("SKU-101", "SKU-050"));
        result.getOutputData().put("browsingCategories", List.of("electronics", "audio"));
        result.getOutputData().put("sessionCount", 14);
        return result;
    }
}
