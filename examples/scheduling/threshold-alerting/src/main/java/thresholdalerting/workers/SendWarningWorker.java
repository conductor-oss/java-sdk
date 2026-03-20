package thresholdalerting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class SendWarningWorker implements Worker {
    @Override public String getTaskDefName() { return "th_send_warning"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [warning] Sending warning for " + task.getInputData().get("metricName"));
        r.getOutputData().put("warned", true);
        r.getOutputData().put("channel", "slack");
        return r;
    }
}
