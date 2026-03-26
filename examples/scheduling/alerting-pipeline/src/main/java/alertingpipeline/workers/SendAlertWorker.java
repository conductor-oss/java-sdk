package alertingpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SendAlertWorker implements Worker {
    @Override public String getTaskDefName() { return "alt_send_alert"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [alert] Firing " + task.getInputData().get("severity") + " alert via " + task.getInputData().get("channel"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("sent", true);
        r.getOutputData().put("alertId", "ALT-20260308-001");
        r.getOutputData().put("channel", task.getInputData().get("channel"));
        return r;
    }
}
