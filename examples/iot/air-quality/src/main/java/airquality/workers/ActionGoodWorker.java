package airquality.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ActionGoodWorker implements Worker {
    @Override public String getTaskDefName() { return "aq_action_good"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [good] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "log");
        r.getOutputData().put("notificationSent", false);
        return r;
    }
}
