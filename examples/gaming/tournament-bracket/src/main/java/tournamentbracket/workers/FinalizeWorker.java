package tournamentbracket.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class FinalizeWorker implements Worker {
    @Override public String getTaskDefName() { return "tbk_finalize"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [finalize] Tournament complete!");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("tournament", Map.of("bracketId", task.getInputData().getOrDefault("bracketId","BRK-743"), "champion", "Alpha", "runnerUp", "Epsilon", "status", "COMPLETED"));
        return result;
    }
}
