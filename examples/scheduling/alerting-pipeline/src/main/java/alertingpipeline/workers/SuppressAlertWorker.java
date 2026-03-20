package alertingpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SuppressAlertWorker implements Worker {
    @Override public String getTaskDefName() { return "alt_suppress_alert"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [suppress] Suppressing alert for " + task.getInputData().get("metricName") + ": " + task.getInputData().get("reason"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("suppressed", true);
        r.getOutputData().put("reason", task.getInputData().get("reason"));
        return r;
    }
}
