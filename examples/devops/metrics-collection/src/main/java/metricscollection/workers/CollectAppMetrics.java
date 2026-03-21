package metricscollection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects application-level metrics such as request rate, error rate, and latency.
 */
public class CollectAppMetrics implements Worker {

    @Override
    public String getTaskDefName() {
        return "mc_collect_app";
    }

    @Override
    public TaskResult execute(Task task) {
        String environment = (String) task.getInputData().get("environment");
        String source = (String) task.getInputData().get("source");

        System.out.println("[mc_collect_app] Collecting application metrics from " + environment);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("metricCount", 45);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("requestRate", 1200);
        metrics.put("errorRate", 0.5);
        metrics.put("p99Latency", 230);
        output.put("metrics", metrics);

        output.put("source", source != null ? source : "application");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
