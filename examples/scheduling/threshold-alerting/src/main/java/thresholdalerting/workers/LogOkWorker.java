package thresholdalerting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class LogOkWorker implements Worker {
    @Override public String getTaskDefName() { return "th_log_ok"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [ok] " + task.getInputData().get("metricName") + " within normal range");
        r.getOutputData().put("logged", true);
        r.getOutputData().put("status", "healthy");
        return r;
    }
}
