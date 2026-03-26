package wiretransfer.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ComplianceCheckWorker implements Worker {
    @Override public String getTaskDefName() { return "wir_compliance_check"; }
    @Override public TaskResult execute(Task task) {
        Object amtObj = task.getInputData().get("amount");
        double amount = amtObj instanceof Number ? ((Number) amtObj).doubleValue() : 0;
        System.out.println("  [compliance] AML/CTR check — amount: $" + amount);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("cleared", true); r.getOutputData().put("ctrRequired", amount >= 10000);
        r.getOutputData().put("sanctionsCleared", true); r.getOutputData().put("riskScore", 12);
        return r;
    }
}
