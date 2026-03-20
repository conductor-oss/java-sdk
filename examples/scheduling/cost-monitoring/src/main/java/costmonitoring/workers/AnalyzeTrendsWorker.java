package costmonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class AnalyzeTrendsWorker implements Worker {
    @Override public String getTaskDefName() { return "cos_analyze_trends"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [analyze] Analyzing spending trends");
        r.getOutputData().put("trend", "increasing");
        r.getOutputData().put("percentOfBudget", 83.0);
        r.getOutputData().put("anomalies", List.of(Map.of("service","compute","spike","+15%")));
        return r;
    }
}
