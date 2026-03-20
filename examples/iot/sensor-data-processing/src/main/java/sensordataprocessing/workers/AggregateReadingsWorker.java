package sensordataprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Aggregates validated sensor readings into summary metrics.
 * Input: batchId, validReadings, validatedData
 * Output: aggregatedMetrics, timeRange
 */
public class AggregateReadingsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sen_aggregate_readings";
    }

    @Override
    public TaskResult execute(Task task) {
        Object validReadings = task.getInputData().get("validReadings");
        System.out.println("  [aggregate] Aggregating " + validReadings + " valid readings");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregatedMetrics", Map.of(
                "avgTemperature", 73.8,
                "maxTemperature", 89.2,
                "minTemperature", 68.1,
                "avgHumidity", 43.5,
                "stdDevTemperature", 4.2
        ));
        result.getOutputData().put("timeRange", Map.of(
                "start", "2026-03-08T09:00:00Z",
                "end", "2026-03-08T10:00:00Z"
        ));
        return result;
    }
}
