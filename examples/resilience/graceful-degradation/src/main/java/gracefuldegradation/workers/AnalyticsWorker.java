package gracefuldegradation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for gd_analytics -- optional analytics tracking step.
 *
 * If available=false, returns { tracked: false }.
 * If available=true (or not specified), returns { tracked: true }.
 */
public class AnalyticsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gd_analytics";
    }

    @Override
    public TaskResult execute(Task task) {
        Object availableInput = task.getInputData().get("available");
        boolean available = toBool(availableInput, true);

        System.out.println("  [gd_analytics] available=" + available);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("tracked", available);
        return result;
    }

    private boolean toBool(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }
}
