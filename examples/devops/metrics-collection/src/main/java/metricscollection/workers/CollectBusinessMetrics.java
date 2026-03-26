package metricscollection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects business-level metrics such as revenue, orders, and conversion rate.
 */
public class CollectBusinessMetrics implements Worker {

    @Override
    public String getTaskDefName() {
        return "mc_collect_business";
    }

    @Override
    public TaskResult execute(Task task) {
        String environment = (String) task.getInputData().get("environment");
        String source = (String) task.getInputData().get("source");

        System.out.println("[mc_collect_business] Collecting business metrics from " + environment);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("metricCount", 18);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("revenue", 24500);
        metrics.put("orders", 340);
        metrics.put("conversionRate", 3.2);
        output.put("metrics", metrics);

        output.put("source", source != null ? source : "business");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
