package leaderboardupdate.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class RankWorker implements Worker {
    @Override public String getTaskDefName() { return "lbu_rank"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [rank] Computing rankings");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("rankings", List.of(Map.of("rank",1,"player","P-101","score",3100), Map.of("rank",2,"player","P-105","score",2950), Map.of("rank",3,"player","P-042","score",2850), Map.of("rank",4,"player","P-103","score",2700)));
        return result;
    }
}
