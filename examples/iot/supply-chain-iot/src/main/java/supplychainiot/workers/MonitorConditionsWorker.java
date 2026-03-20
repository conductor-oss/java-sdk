package supplychainiot.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class MonitorConditionsWorker implements Worker {
    @Override public String getTaskDefName() { return "sci_monitor_conditions"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [conditions] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("temperature", "temp");
        r.getOutputData().put("conditionStatus", "status");
        return r;
    }
}
