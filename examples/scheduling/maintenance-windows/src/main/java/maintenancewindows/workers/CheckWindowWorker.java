package maintenancewindows.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckWindowWorker implements Worker {
    @Override public String getTaskDefName() { return "mnw_check_window"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [check] Checking maintenance window for " + task.getInputData().get("system") + " at " + task.getInputData().get("currentTime"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("windowStatus", "in_window");
        r.getOutputData().put("windowStart", "2026-03-08T02:00:00Z");
        r.getOutputData().put("windowEnd", "2026-03-08T06:00:00Z");
        r.getOutputData().put("nextWindowStart", "2026-03-15T02:00:00Z");
        r.getOutputData().put("remainingMinutes", 120);
        return r;
    }
}
