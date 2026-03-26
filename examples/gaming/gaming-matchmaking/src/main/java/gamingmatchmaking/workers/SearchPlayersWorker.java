package gamingmatchmaking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class SearchPlayersWorker implements Worker {
    @Override public String getTaskDefName() { return "gmm_search_players"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [search] Searching " + task.getInputData().get("region") + " players for " + task.getInputData().get("gameMode"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("candidates", List.of("P-101", "P-102", "P-103", "P-104", "P-105"));
        result.addOutputData("count", 5);
        return result;
    }
}
