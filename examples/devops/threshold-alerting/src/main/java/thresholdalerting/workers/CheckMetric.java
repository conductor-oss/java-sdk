package thresholdalerting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Checks a metric value against warning and critical thresholds,
 * determining the severity level.
 */
public class CheckMetric implements Worker {

    @Override
    public String getTaskDefName() {
        return "th_check_metric";
    }

    @Override
    public TaskResult execute(Task task) {
        double value = toDouble(task.getInputData().get("currentValue"), 0);
        double warn = toDouble(task.getInputData().get("warningThreshold"), 70);
        double crit = toDouble(task.getInputData().get("criticalThreshold"), 90);

        String severity = "ok";
        if (value >= crit) severity = "critical";
        else if (value >= warn) severity = "warning";

        String metricName = (String) task.getInputData().get("metricName");
        System.out.println("[th_check_metric] " + metricName + ": " + value + " (warn: " + warn + ", crit: " + crit + ") -> " + severity);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("severity", severity);
        result.getOutputData().put("value", value);
        result.getOutputData().put("warningThreshold", warn);
        result.getOutputData().put("criticalThreshold", crit);
        return result;
    }

    private double toDouble(Object val, double defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return defaultVal; }
    }
}
