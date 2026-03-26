package autoscaling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Analyzes current service metrics (CPU, memory, request rate) to determine load.
 */
public class Analyze implements Worker {

    @Override
    public String getTaskDefName() {
        return "as_analyze";
    }

    @Override
    public TaskResult execute(Task task) {
        String service = (String) task.getInputData().get("service");
        String metric = (String) task.getInputData().get("metric");

        if (service == null) service = "unknown-service";
        if (metric == null) metric = "cpu";

        System.out.println("[as_analyze] Analyzing " + metric + " for " + service);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("service", service);
        output.put("metric", metric);

        // Deterministic load values based on service name
        int currentLoad;
        switch (service) {
            case "api-server":    currentLoad = 85; break;
            case "web-frontend":  currentLoad = 45; break;
            case "batch-worker":  currentLoad = 92; break;
            case "cache-service": currentLoad = 30; break;
            default:              currentLoad = 60; break;
        }

        output.put("currentLoad", currentLoad);
        output.put("avgLoad15m", currentLoad - 5);
        output.put("peakLoad1h", currentLoad + 8);
        output.put("currentInstances", 3);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
