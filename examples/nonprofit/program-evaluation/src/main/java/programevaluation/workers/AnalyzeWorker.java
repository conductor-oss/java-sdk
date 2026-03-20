package programevaluation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AnalyzeWorker implements Worker {
    @Override public String getTaskDefName() { return "pev_analyze"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [analyze] Analyzing program performance");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("results", Map.of("reachScore", 88, "outcomeScore", 82, "efficiencyScore", 76, "satisfactionScore", 92, "overall", 84.5)); return r;
    }
}
