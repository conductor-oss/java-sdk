package leaderboardupdate.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class UpdateWorker implements Worker {
    @Override public String getTaskDefName() { return "lbu_update"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [update] Leaderboard updated for season " + task.getInputData().get("season"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("leaderboardId", "LB-742");
        result.addOutputData("updated", true);
        return result;
    }
}
