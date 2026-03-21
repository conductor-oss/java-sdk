package environmentalmonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class TriggerAlertWorker implements Worker {
    @Override public String getTaskDefName() { return "env_trigger_alert"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [alert] Processing " + task.getInputData().getOrDefault("alertsSent", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("alertsSent", 2);
        r.getOutputData().put("notifications", List.of("email:env-team@example.com", "dashboard:env-monitor"));
        r.getOutputData().put("escalated", true);
        return r;
    }
}
