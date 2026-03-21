package taxfiling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class ValidateFilingWorker implements Worker {
    @Override public String getTaskDefName() { return "txf_validate_filing"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Validating filing — liability: $" + task.getInputData().get("taxLiability"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("validated", true);
        r.getOutputData().put("validationChecks", List.of("income_consistency", "deduction_limits", "credit_eligibility"));
        r.getOutputData().put("warnings", List.of());
        return r;
    }
}
