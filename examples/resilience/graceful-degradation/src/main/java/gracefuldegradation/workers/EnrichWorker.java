package gracefuldegradation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for gd_enrich -- optional enrichment step.
 *
 * If available=false, returns { enriched: false }.
 * If available=true (or not specified), returns { enriched: true }.
 */
public class EnrichWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gd_enrich";
    }

    @Override
    public TaskResult execute(Task task) {
        Object availableInput = task.getInputData().get("available");
        boolean available = toBool(availableInput, true);

        System.out.println("  [gd_enrich] available=" + available);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enriched", available);
        return result;
    }

    private boolean toBool(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }
}
