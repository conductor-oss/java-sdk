package metricscollection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregates metric counts from all sources into a combined total.
 */
public class AggregateMetrics implements Worker {

    @Override
    public String getTaskDefName() {
        return "mc_aggregate";
    }

    @Override
    public TaskResult execute(Task task) {
        int appMetrics = toInt(task.getInputData().get("appMetrics"), 0);
        int infraMetrics = toInt(task.getInputData().get("infraMetrics"), 0);
        int bizMetrics = toInt(task.getInputData().get("bizMetrics"), 0);
        int total = appMetrics + infraMetrics + bizMetrics;

        System.out.println("[mc_aggregate] Aggregating " + total + " metrics from 3 sources");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("totalMetrics", total);
        output.put("sources", 3);
        output.put("aggregatedAt", "2026-03-08T06:00:00Z");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }

    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return defaultVal; }
    }
}
