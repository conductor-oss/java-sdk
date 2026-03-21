package donormanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AcquireWorker implements Worker {
    @Override public String getTaskDefName() { return "dnr_acquire"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [acquire] New donor: " + task.getInputData().get("donorName") + " (" + task.getInputData().get("donorEmail") + ")");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("donorId", "DNR-755"); r.addOutputData("acquired", true); return r;
    }
}
