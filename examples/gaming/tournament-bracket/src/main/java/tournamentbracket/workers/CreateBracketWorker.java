package tournamentbracket.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CreateBracketWorker implements Worker {
    @Override public String getTaskDefName() { return "tbk_create_bracket"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [bracket] Creating " + task.getInputData().get("format") + " bracket");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("bracketId", "BRK-743");
        result.addOutputData("rounds", 3);
        result.addOutputData("matches", 7);
        return result;
    }
}
