package portfoliorebalancing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class AnalyzeDriftWorker implements Worker {
    @Override public String getTaskDefName() { return "prt_analyze_drift"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [drift] Analyzing drift for portfolio " + task.getInputData().get("portfolioId") + ", strategy: " + task.getInputData().get("strategy"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("driftAnalysis", List.of(
            Map.of("asset", "US Equities", "target", 60, "current", 65.2, "drift", 5.2),
            Map.of("asset", "Int'l Equities", "target", 20, "current", 17.8, "drift", -2.2),
            Map.of("asset", "Fixed Income", "target", 15, "current", 12.5, "drift", -2.5),
            Map.of("asset", "Alternatives", "target", 5, "current", 4.5, "drift", -0.5)
        ));
        result.getOutputData().put("maxDrift", 5.2);
        result.getOutputData().put("rebalanceNeeded", true);
        return result;
    }
}
