package rootcauseanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class IdentifyRootCauseWorker implements Worker {
    @Override public String getTaskDefName() { return "rca_identify_root_cause"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [root-cause] Root cause identified (confidence: " + task.getInputData().get("confidence") + "%)");
        r.getOutputData().put("rootCause", task.getInputData().get("topCandidate"));
        r.getOutputData().put("confirmed", true);
        r.getOutputData().put("remediation", "Rollback auth-service to v2.3.0");
        return r;
    }
}
