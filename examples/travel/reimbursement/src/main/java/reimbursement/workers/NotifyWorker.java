package reimbursement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class NotifyWorker implements Worker {
    @Override public String getTaskDefName() { return "rmb_notify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [notify] " + task.getInputData().get("employeeId") + " notified: $" + task.getInputData().get("amount") + " reimbursed");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("notified", true); return r;
    }
}
