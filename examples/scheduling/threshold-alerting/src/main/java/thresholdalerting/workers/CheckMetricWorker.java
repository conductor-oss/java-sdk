package thresholdalerting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CheckMetricWorker implements Worker {
    @Override public String getTaskDefName() { return "th_check_metric"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        double value = 0, warn = 70, crit = 90;
        try { value = Double.parseDouble(String.valueOf(task.getInputData().get("currentValue"))); } catch (Exception ignored) {}
        try { warn = Double.parseDouble(String.valueOf(task.getInputData().get("warningThreshold"))); } catch (Exception ignored) {}
        try { crit = Double.parseDouble(String.valueOf(task.getInputData().get("criticalThreshold"))); } catch (Exception ignored) {}
        String severity = "ok";
        if (value >= crit) severity = "critical";
        else if (value >= warn) severity = "warning";
        System.out.println("  [check] " + task.getInputData().get("metricName") + ": " + value + " -> " + severity);
        r.getOutputData().put("severity", severity);
        r.getOutputData().put("value", value);
        return r;
    }
}
