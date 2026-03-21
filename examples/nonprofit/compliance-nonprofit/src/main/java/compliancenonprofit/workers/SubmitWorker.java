package compliancenonprofit.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class SubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "cnp_submit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Submitting compliance package for EIN " + task.getInputData().get("ein"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("submission", Map.of("ein", task.getInputData().getOrDefault("ein","12-3456789"), "status", "SUBMITTED", "confirmationId", "COMP-760", "compliant", true)); return r;
    }
}
