package leaderboardupdate.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class BroadcastWorker implements Worker {
    @Override public String getTaskDefName() { return "lbu_broadcast"; }
    @Override public TaskResult execute(Task task) {
        String lbId = (String) task.getInputData().get("leaderboardId");
        System.out.println("  [broadcast] Broadcasting leaderboard " + lbId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("leaderboard", Map.of("id", lbId != null ? lbId : "LB-742", "top3", List.of("P-101","P-105","P-042"), "status", "LIVE"));
        return result;
    }
}
