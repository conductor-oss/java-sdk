package agricultureiot.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class IrrigationDecisionWorker implements Worker {
    @Override public String getTaskDefName() { return "agr_irrigation_decision"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [decision] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("durationMinutes", "duration");
        r.getOutputData().put("zones", java.util.List.of("zone-A"));
        return r;
    }
}
