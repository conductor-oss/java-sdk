package playerprogression.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class NotifyWorker implements Worker {
    @Override public String getTaskDefName() { return "ppg_notify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [notify] Notifying " + task.getInputData().get("playerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("progression", Map.of("playerId", task.getInputData().getOrDefault("playerId","P-042"), "rewards", task.getInputData().getOrDefault("rewards", java.util.List.of()), "notified", true));
        return r;
    }
}
