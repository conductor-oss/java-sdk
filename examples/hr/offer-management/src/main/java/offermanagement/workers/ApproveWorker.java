package offermanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class ApproveWorker implements Worker {
    @Override public String getTaskDefName() { return "ofm_approve"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [approve] Offer " + task.getInputData().get("offerId") + " approved by VP and HR");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("approved", true);
        r.getOutputData().put("approvers", List.of("VP", "HR"));
        return r;
    }
}
