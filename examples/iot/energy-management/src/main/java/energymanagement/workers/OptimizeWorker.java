package energymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Generates optimization recommendations and projected savings.
 * Input: buildingId, patterns, peakHours
 * Output: projectedSavings, recommendations
 */
public class OptimizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "erg_optimize";
    }

    @Override
    public TaskResult execute(Task task) {
        String buildingId = (String) task.getInputData().getOrDefault("buildingId", "unknown");

        System.out.println("  [optimize] Generating optimization plan for " + buildingId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("projectedSavings", "18.5%");
        result.getOutputData().put("recommendations", List.of(
                "Shift HVAC pre-cooling to off-peak hours",
                "Reduce lighting in unoccupied zones",
                "Enable demand response during peak"
        ));
        return result;
    }
}
