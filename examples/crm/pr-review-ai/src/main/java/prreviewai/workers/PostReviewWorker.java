package prreviewai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class PostReviewWorker implements Worker {
    @Override public String getTaskDefName() { return "prr_post_review"; }
    @Override public TaskResult execute(Task task) {
        Object prNumber = task.getInputData().getOrDefault("prNumber", 0);
        System.out.println("  [post] Posted review on PR #" + prNumber);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("posted", true);
        return result;
    }
}
