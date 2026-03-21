package tournamentbracket.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class RegisterWorker implements Worker {
    @Override public String getTaskDefName() { return "tbk_register"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [register] Registering players for " + task.getInputData().get("tournamentName"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("players", List.of("Alpha","Beta","Gamma","Delta","Epsilon","Zeta","Eta","Theta"));
        result.addOutputData("count", 8);
        return result;
    }
}
