package energymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Analyzes energy usage patterns and identifies peak hours.
 * Input: buildingId, consumption
 * Output: patterns, peakHours, baselineKw
 */
public class AnalyzePatternsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "erg_analyze_patterns";
    }

    @Override
    public TaskResult execute(Task task) {
        String buildingId = (String) task.getInputData().getOrDefault("buildingId", "unknown");

        System.out.println("  [analyze] Identifying usage patterns for " + buildingId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("patterns", List.of("high-midday-usage", "low-overnight", "hvac-dominant"));
        result.getOutputData().put("peakHours", List.of("11:00-15:00"));
        result.getOutputData().put("baselineKw", 12.3);
        return result;
    }
}
