package donormanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class UpgradeWorker implements Worker {
    @Override public String getTaskDefName() { return "dnr_upgrade"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [upgrade] Evaluating upgrade for " + task.getInputData().get("donorId") + " from " + task.getInputData().get("currentLevel"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("donor", Map.of("donorId", task.getInputData().getOrDefault("donorId","DNR-755"), "level", "major-donor", "upgraded", true, "status", "ACTIVE")); return r;
    }
}
