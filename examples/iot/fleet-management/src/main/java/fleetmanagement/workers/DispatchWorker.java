package fleetmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DispatchWorker implements Worker {
    @Override public String getTaskDefName() { return "flt_dispatch"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [dispatch] Processing " + task.getInputData().getOrDefault("dispatchId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("dispatchId", "DSP-535-001");
        r.getOutputData().put("dispatchedAt", "2026-03-08T09:00:00Z");
        r.getOutputData().put("etaMinutes", 42);
        r.getOutputData().put("driverNotified", true);
        return r;
    }
}
