package humanuserassignment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for hua_post_review — finalizes the document after human review.
 *
 * Returns { finalized: true } on success.
 */
public class HuaPostReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hua_post_review";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [hua_post_review] Finalizing document after review...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalized", true);

        return result;
    }
}
