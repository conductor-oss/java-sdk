package trainingmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CertifyWorker implements Worker {
    @Override public String getTaskDefName() { return "trm_certify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [certify] Certification issued to " + task.getInputData().get("employeeId") + " (score: " + task.getInputData().get("score") + ")");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("certificationId", "CERT-700");
        r.getOutputData().put("expiresIn", "2 years");
        return r;
    }
}
