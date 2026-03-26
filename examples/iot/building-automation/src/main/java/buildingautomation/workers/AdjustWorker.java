package buildingautomation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AdjustWorker implements Worker {
    @Override public String getTaskDefName() { return "bld_adjust"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [adjust] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("applied", true);
        r.getOutputData().put("adjustmentCount", 3);
        return r;
    }
}
