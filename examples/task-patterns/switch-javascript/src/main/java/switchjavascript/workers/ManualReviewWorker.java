package switchjavascript.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles high-value orders that require manual review (amount > 5000, non-VIP).
 */
public class ManualReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "swjs_manual_review";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [manual_review] High-value order flagged for review");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "swjs_manual_review");
        result.getOutputData().put("processed", true);
        return result;
    }
}
