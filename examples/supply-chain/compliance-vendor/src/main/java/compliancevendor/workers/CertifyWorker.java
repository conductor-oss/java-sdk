package compliancevendor.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CertifyWorker implements Worker {
    @Override public String getTaskDefName() { return "vcm_certify"; }
    @Override public TaskResult execute(Task task) {
        Object ap = task.getInputData().get("auditPassed");
        boolean auditPassed = Boolean.TRUE.equals(ap) || "true".equals(String.valueOf(ap));
        String certId = auditPassed ? "CERT-665-001" : null;
        System.out.println("  [certify] " + (certId != null ? "Certification issued: " + certId : "Certification denied"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("certificationId", certId); r.getOutputData().put("validUntil", "2025-12-31"); return r;
    }
}
