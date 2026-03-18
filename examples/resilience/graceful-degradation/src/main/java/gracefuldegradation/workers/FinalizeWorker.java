package gracefuldegradation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for gd_finalize -- checks enrichment and analytics results,
 * sets a degraded flag if either service was unavailable.
 *
 * Inputs: enriched (boolean), analytics (boolean)
 * Output: degraded = !enriched || !analytics
 */
public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gd_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        boolean enriched = toBool(task.getInputData().get("enriched"), false);
        boolean analytics = toBool(task.getInputData().get("analytics"), false);
        boolean degraded = !enriched || !analytics;

        System.out.println("  [gd_finalize] enriched=" + enriched + " analytics=" + analytics + " degraded=" + degraded);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enriched", enriched);
        result.getOutputData().put("analytics", analytics);
        result.getOutputData().put("degraded", degraded);
        return result;
    }

    private boolean toBool(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }
}
