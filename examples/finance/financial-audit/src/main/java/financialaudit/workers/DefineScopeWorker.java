package financialaudit.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class DefineScopeWorker implements Worker {
    @Override public String getTaskDefName() { return "fau_define_scope"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [scope] Audit " + task.getInputData().get("auditId") + ": " + task.getInputData().get("auditType") + " for " + task.getInputData().get("entityName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("scopeAreas", List.of("revenue_recognition","accounts_payable","inventory","payroll"));
        r.getOutputData().put("materialityThreshold", 50000); r.getOutputData().put("riskLevel", "medium");
        return r;
    }
}
