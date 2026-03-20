package bidmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CreateWorker implements Worker {
    @Override public String getTaskDefName() { return "bid_create"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [create] Bid for \"" + task.getInputData().get("projectName") + "\" — budget: $" + task.getInputData().get("budget"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("bidId", "BID-663-001"); return r;
    }
}
