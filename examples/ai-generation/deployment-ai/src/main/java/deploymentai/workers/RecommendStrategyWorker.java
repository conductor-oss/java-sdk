package deploymentai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class RecommendStrategyWorker implements Worker {
    @Override public String getTaskDefName() { return "dai_recommend_strategy"; }
    @Override public TaskResult execute(Task task) {
        String riskLevel = (String) task.getInputData().getOrDefault("riskLevel", "low");
        String environment = (String) task.getInputData().getOrDefault("environment", "staging");
        Map<String, String> strategies = Map.of("high", "canary", "medium", "blue-green", "low", "rolling", "minimal", "rolling");
        String strategy = strategies.getOrDefault(riskLevel, "rolling");
        System.out.println("  [strategy] Recommended: " + strategy + " for " + environment);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("strategy", strategy);
        return result;
    }
}
