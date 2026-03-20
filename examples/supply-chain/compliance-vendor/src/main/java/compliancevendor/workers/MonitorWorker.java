package compliancevendor.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class MonitorWorker implements Worker {
    @Override public String getTaskDefName() { return "vcm_monitor"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [monitor] Continuous monitoring enabled for " + task.getInputData().get("vendorId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("active", true); r.getOutputData().put("checkInterval", "monthly"); return r;
    }
}
