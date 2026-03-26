package buildingautomation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class MonitorSystemsWorker implements Worker {
    @Override public String getTaskDefName() { return "bld_monitor_systems"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [monitor] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("hvacTemp", "hvac.currentTemp");
        return r;
    }
}
