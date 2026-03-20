package eventmonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Analyzes throughput from raw metrics.
 * Input: metrics (rawMetrics map)
 * Output: throughput (eventsPerSec, totalEvents)
 */
public class AnalyzeThroughputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "em_analyze_throughput";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> metrics = (Map<String, Object>) task.getInputData().get("metrics");

        int totalEvents = 0;
        if (metrics != null && metrics.get("totalEvents") != null) {
            totalEvents = ((Number) metrics.get("totalEvents")).intValue();
        }

        System.out.println("  [em_analyze_throughput] Analyzing throughput for " + totalEvents + " total events");

        Map<String, Object> throughput = Map.of(
                "eventsPerSec", "51.4",
                "totalEvents", totalEvents
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("throughput", throughput);
        return result;
    }
}
