package rfpautomation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DistributeWorker implements Worker {
    @Override public String getTaskDefName() { return "rfp_distribute"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [distribute] " + task.getInputData().get("rfpId") + " sent to qualified vendors — deadline: " + task.getInputData().get("deadline"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("distributedTo", 5); return r;
    }
}
