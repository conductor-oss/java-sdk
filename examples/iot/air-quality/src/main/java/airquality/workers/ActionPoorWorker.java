package airquality.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ActionPoorWorker implements Worker {
    @Override public String getTaskDefName() { return "aq_action_poor"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [POOR] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "health_warning");
        r.getOutputData().put("notificationSent", true);
        return r;
    }
}
