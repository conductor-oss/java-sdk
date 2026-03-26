package leaderboardupdate.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class CollectScoresWorker implements Worker {
    @Override public String getTaskDefName() { return "lbu_collect_scores"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Collecting scores for game " + task.getInputData().get("gameId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("scores", List.of(Map.of("player","P-042","score",2850), Map.of("player","P-101","score",3100), Map.of("player","P-103","score",2700), Map.of("player","P-105","score",2950)));
        return result;
    }
}
