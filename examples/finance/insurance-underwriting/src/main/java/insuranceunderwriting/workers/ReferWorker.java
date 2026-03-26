package insuranceunderwriting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReferWorker implements Worker {
    @Override public String getTaskDefName() { return "uw_refer"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [refer] Application " + task.getInputData().get("applicationId") + " REFERRED: " + task.getInputData().get("referReason"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("referred", true); r.getOutputData().put("assignedTo", "Senior Underwriter");
        return r;
    }
}
