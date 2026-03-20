package custommetrics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Collects data points for registered custom metrics.
 */
public class CollectData implements Worker {

    @Override
    public String getTaskDefName() {
        return "cus_collect_data";
    }

    @Override
    public TaskResult execute(Task task) {
        Object registeredMetrics = task.getInputData().get("registeredMetrics");
        String interval = (String) task.getInputData().get("collectionInterval");

        System.out.println("[cus_collect_data] Collecting data for " + registeredMetrics + " custom metrics (interval: " + interval + ")...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rawDataPoints", 4800);
        result.getOutputData().put("collectedAt", "2026-03-08T06:00:00Z");
        result.getOutputData().put("metricsWithData", 4);
        return result;
    }
}
