package rfpautomation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CreateWorker implements Worker {
    @Override public String getTaskDefName() { return "rfp_create"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [create] RFP: \"" + task.getInputData().get("projectTitle") + "\"");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("rfpId", "RFP-664-001"); return r;
    }
}
