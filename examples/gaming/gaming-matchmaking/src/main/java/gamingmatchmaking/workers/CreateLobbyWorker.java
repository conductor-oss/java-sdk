package gamingmatchmaking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CreateLobbyWorker implements Worker {
    @Override public String getTaskDefName() { return "gmm_create_lobby"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [lobby] Creating lobby for match " + task.getInputData().get("matchId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("lobbyId", "LOBBY-3301");
        result.addOutputData("playerCount", 4);
        result.addOutputData("server", "us-west-2");
        return result;
    }
}
