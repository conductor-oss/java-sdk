package predictivemaintenance.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class ScheduleMaintenanceWorker implements Worker {
    @Override public String getTaskDefName() { return "pmn_schedule_maintenance"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [schedule] Processing " + task.getInputData().getOrDefault("maintenanceDate", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("maintenanceDate", "2026-05-10");
        r.getOutputData().put("workOrderId", "WO-538-001");
        r.getOutputData().put("estimatedCost", 4500);
        r.getOutputData().put("partsOrdered", List.of("compressor_valve_CV-300"));
        return r;
    }
}
