package uptimemonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class LogResultWorker implements Worker {
    @Override public String getTaskDefName() { return "um_log_result"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [log] Logging: " + task.getInputData().get("endpoint") + " -> " + task.getInputData().get("status"));
        r.getOutputData().put("logged", true);
        return r;
    }
}
