package rfpautomation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class SelectWorker implements Worker {
    @Override public String getTaskDefName() { return "rfp_select"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [select] Selected " + task.getInputData().get("topCandidate") + " for " + task.getInputData().get("rfpId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("selectedVendor", task.getInputData().get("topCandidate")); r.getOutputData().put("notified", true); return r;
    }
}
