package buildingautomation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class OptimizeWorker implements Worker {
    @Override public String getTaskDefName() { return "bld_optimize"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [optimize] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("energySaved", "12.3%");
        r.getOutputData().put("costSavings", "$45/day");
        return r;
    }
}
