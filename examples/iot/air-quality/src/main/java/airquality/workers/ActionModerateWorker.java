package airquality.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ActionModerateWorker implements Worker {
    @Override public String getTaskDefName() { return "aq_action_moderate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [moderate] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "advisory");
        r.getOutputData().put("notificationSent", true);
        return r;
    }
}
