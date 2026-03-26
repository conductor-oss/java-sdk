package userbehavioranalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class AnalyzePatternsWorker implements Worker {
    @Override public String getTaskDefName() { return "uba_analyze_patterns"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [analyze] Analyzing patterns across " + task.getInputData().get("sessionCount") + " sessions");
        r.getOutputData().put("riskScore", 78);
        r.getOutputData().put("anomalies", List.of(Map.of("type","unusual_access_time","severity","medium")));
        r.getOutputData().put("baselineDeviation", 2.4);
        return r;
    }
}
