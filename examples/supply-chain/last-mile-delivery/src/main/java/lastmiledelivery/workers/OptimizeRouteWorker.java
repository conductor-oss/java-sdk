package lastmiledelivery.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class OptimizeRouteWorker implements Worker {
    @Override public String getTaskDefName() { return "lmd_optimize_route"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [route] Optimized route for " + task.getInputData().get("driverId") + " -> " + task.getInputData().get("address"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("route", "via I-90 to Main St"); r.getOutputData().put("estimatedTime", "25 min"); r.getOutputData().put("distance", "8.3 km"); return r;
    }
}
