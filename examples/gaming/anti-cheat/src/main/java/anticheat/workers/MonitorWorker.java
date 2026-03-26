package anticheat.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class MonitorWorker implements Worker {
    @Override public String getTaskDefName() { return "ach_monitor"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [monitor] Monitoring player " + task.getInputData().get("playerId") + " in match " + task.getInputData().get("matchId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("metrics", Map.of("aimAccuracy", 0.45, "reactionTime", 180, "headshotRatio", 0.35));
        return r;
    }
}
