package gamingmatchmaking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class StartWorker implements Worker {
    @Override public String getTaskDefName() { return "gmm_start"; }
    @Override public TaskResult execute(Task task) {
        String lobbyId = (String) task.getInputData().get("lobbyId");
        System.out.println("  [start] Game started in lobby " + lobbyId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("game", Map.of("lobbyId", lobbyId != null ? lobbyId : "LOBBY-3301", "status", "IN_PROGRESS", "map", "Arena-7"));
        return result;
    }
}
