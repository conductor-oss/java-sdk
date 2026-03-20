package grantmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ApproveWorker implements Worker {
    @Override public String getTaskDefName() { return "gmt_approve"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [approve] Score: " + task.getInputData().get("reviewScore") + " - approved");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("approved", true); r.addOutputData("approvedAmount", task.getInputData().get("requestedAmount"));
        return r;
    }
}
