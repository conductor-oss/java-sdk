package virtualeconomy.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AuditWorker implements Worker {
    @Override public String getTaskDefName() { return "vec_audit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [audit] Audit log: txn=" + task.getInputData().get("transactionId") + ", balance=" + task.getInputData().get("newBalance"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("audited", true); r.addOutputData("auditId", "AUD-750");
        return r;
    }
}
