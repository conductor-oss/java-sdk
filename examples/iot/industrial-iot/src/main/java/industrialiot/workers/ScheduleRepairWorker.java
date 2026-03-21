package industrialiot.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class ScheduleRepairWorker implements Worker {
    @Override public String getTaskDefName() { return "iit_schedule_repair"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [repair] Processing " + task.getInputData().getOrDefault("repairScheduled", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("repairScheduled", "scheduled");
        r.getOutputData().put("workOrderId", "WO-537-001");
        r.getOutputData().put("scheduledDate", "2026-04-15");
        r.getOutputData().put("partsRequired", List.of("bearing_kit_BK-200"));
        r.getOutputData().put("estimatedDowntime", "4 hours");
        return r;
    }
}
