package inapppurchase.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class VerifyWorker implements Worker {
    @Override public String getTaskDefName() { return "iap_verify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [verify] Verifying purchase eligibility for " + task.getInputData().get("playerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("eligible", true); r.addOutputData("ageVerified", true);
        return r;
    }
}
