package rootcauseanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class RcaAnalyzeWorker implements Worker {
    @Override public String getTaskDefName() { return "rca_analyze"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [analyze] Analyzing " + task.getInputData().get("logs") + " logs and " + task.getInputData().get("metrics") + " metrics");
        r.getOutputData().put("topCandidate", "auth-service deployment v2.3.1 introduced connection pool exhaustion");
        r.getOutputData().put("confidence", 92);
        return r;
    }
}
