package fleetmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MonitorTripWorker implements Worker {
    @Override public String getTaskDefName() { return "flt_monitor_trip"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [monitor] Processing " + task.getInputData().getOrDefault("tripStatus", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("tripStatus", "completed");
        r.getOutputData().put("actualDistance", 29.1);
        r.getOutputData().put("actualDuration", 45);
        r.getOutputData().put("fuelUsed", 3.4);
        r.getOutputData().put("avgSpeed", 38.8);
        r.getOutputData().put("completedAt", "2026-03-08T09:45:00Z");
        return r;
    }
}
