package compliancemonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class EvaluatePoliciesWorker implements Worker {
    @Override public String getTaskDefName() { return "cpm_evaluate_policies"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [evaluate] Evaluating " + task.getInputData().get("resourcesScanned") + " resources");
        r.getOutputData().put("overallStatus", "violation");
        r.getOutputData().put("complianceScore", 87);
        r.getOutputData().put("violationCount", 3);
        r.getOutputData().put("violations", List.of(Map.of("rule","encryption-at-rest","severity","high")));
        return r;
    }
}
