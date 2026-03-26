package taskassignment.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class NotifyWorker implements Worker {
    @Override public String getTaskDefName() { return "tas_notify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [notify] Notified " + task.getInputData().get("assignee") + " about task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("notified", true); r.getOutputData().put("channel", "email"); return r;
    }
}
