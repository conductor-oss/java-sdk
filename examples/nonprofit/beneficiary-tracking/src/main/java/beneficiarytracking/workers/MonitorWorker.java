package beneficiarytracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class MonitorWorker implements Worker {
    @Override public String getTaskDefName() { return "btr_monitor"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [monitor] Monitoring progress for " + task.getInputData().get("beneficiaryId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("outcomes", Map.of("foodSecurity", "improved", "healthStatus", "good", "academicProgress", "on-track")); return r;
    }
}
