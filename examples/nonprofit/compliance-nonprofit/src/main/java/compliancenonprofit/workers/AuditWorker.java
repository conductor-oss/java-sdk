package compliancenonprofit.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AuditWorker implements Worker {
    @Override public String getTaskDefName() { return "cnp_audit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [audit] Auditing " + task.getInputData().get("organizationName") + " for FY" + task.getInputData().get("fiscalYear"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("results", Map.of("financials", "clean", "governance", "compliant", "programExpenseRatio", 0.82)); return r;
    }
}
