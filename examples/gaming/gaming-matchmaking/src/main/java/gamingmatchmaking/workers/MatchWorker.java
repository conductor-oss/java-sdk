package gamingmatchmaking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class MatchWorker implements Worker {
    @Override public String getTaskDefName() { return "gmm_match"; }
    @Override public TaskResult execute(Task task) {
        String playerId = (String) task.getInputData().get("playerId");
        System.out.println("  [match] Creating balanced match for player " + playerId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("matchId", "MATCH-741");
        result.addOutputData("players", List.of(playerId != null ? playerId : "P-042", "P-101", "P-103", "P-105"));
        return result;
    }
}
