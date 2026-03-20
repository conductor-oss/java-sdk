package leaderboardupdate.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "lbu_validate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Validating scores");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("validScores", task.getInputData().get("scores"));
        result.addOutputData("rejected", 0);
        return result;
    }
}
