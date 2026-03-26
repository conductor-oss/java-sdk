package grantmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReviewWorker implements Worker {
    @Override public String getTaskDefName() { return "gmt_review"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [review] Reviewing application for $" + task.getInputData().get("requestedAmount"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("score", 87); r.addOutputData("recommendation", "approve"); r.addOutputData("reviewer", "Grant Committee");
        return r;
    }
}
