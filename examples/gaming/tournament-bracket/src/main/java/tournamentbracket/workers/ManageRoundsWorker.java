package tournamentbracket.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ManageRoundsWorker implements Worker {
    @Override public String getTaskDefName() { return "tbk_manage_rounds"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [rounds] Managing rounds for bracket " + task.getInputData().get("bracketId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("results", Map.of("round1", List.of("Alpha","Gamma","Epsilon","Theta"), "semiFinal", List.of("Alpha","Epsilon"), "final", List.of("Alpha")));
        return result;
    }
}
