package anticheat.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ActWorker implements Worker {
    @Override public String getTaskDefName() { return "ach_act"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [act] Final action for " + task.getInputData().get("playerId") + ": verdict=" + task.getInputData().get("verdict"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("action", Map.of("playerId", task.getInputData().getOrDefault("playerId","P-042"), "verdict", task.getInputData().getOrDefault("verdict","clean"), "processed", true));
        return r;
    }
}
