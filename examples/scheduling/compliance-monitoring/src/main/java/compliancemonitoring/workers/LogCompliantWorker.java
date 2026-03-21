package compliancemonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class LogCompliantWorker implements Worker {
    @Override public String getTaskDefName() { return "cpm_log_compliant"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [compliant] All " + task.getInputData().get("framework") + " checks passed");
        r.getOutputData().put("logged", true);
        r.getOutputData().put("certificateIssued", true);
        return r;
    }
}
