package campaignautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class TargetWorker implements Worker {
    @Override public String getTaskDefName() { return "cpa_target"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [target] Audience built for campaign " + task.getInputData().get("campaignId") + " (budget: $" + task.getInputData().get("budget") + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("audience", Map.of(
                "segments", List.of("tech-professionals", "decision-makers"),
                "filters", Map.of("region", "US", "age", "25-54")));
        result.getOutputData().put("audienceSize", 125000);
        return result;
    }
}
