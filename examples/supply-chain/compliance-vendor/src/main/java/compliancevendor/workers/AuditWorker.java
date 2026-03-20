package compliancevendor.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AuditWorker implements Worker {
    @Override public String getTaskDefName() { return "vcm_audit"; }
    @Override public TaskResult execute(Task task) {
        boolean passed = "satisfactory".equals(task.getInputData().get("assessmentResult"));
        System.out.println("  [audit] Audit " + (passed ? "passed" : "failed") + " — 3 findings, all minor");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("passed", passed); r.getOutputData().put("findings", 3); r.getOutputData().put("critical", 0); return r;
    }
}
