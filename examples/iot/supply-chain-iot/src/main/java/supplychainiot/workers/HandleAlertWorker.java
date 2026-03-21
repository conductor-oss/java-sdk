package supplychainiot.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class HandleAlertWorker implements Worker {
    @Override public String getTaskDefName() { return "sci_handle_alert"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [ALERT] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "reroute");
        r.getOutputData().put("alertSent", true);
        return r;
    }
}
