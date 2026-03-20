package bidmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AwardWorker implements Worker {
    @Override public String getTaskDefName() { return "bid_award"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [award] " + task.getInputData().get("bidId") + " awarded to " + task.getInputData().get("winner"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("awardedTo", task.getInputData().get("winner")); r.getOutputData().put("notified", true); return r;
    }
}
