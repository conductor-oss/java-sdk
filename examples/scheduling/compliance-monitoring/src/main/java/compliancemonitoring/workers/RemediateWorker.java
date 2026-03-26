package compliancemonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class RemediateWorker implements Worker {
    @Override public String getTaskDefName() { return "cpm_remediate"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        boolean auto = "true".equals(String.valueOf(task.getInputData().get("autoRemediate")));
        System.out.println("  [remediate] Violations found - auto-remediate: " + auto);
        r.getOutputData().put("remediationStarted", auto);
        r.getOutputData().put("ticketsCreated", 3);
        r.getOutputData().put("autoFixed", auto ? 2 : 0);
        return r;
    }
}
