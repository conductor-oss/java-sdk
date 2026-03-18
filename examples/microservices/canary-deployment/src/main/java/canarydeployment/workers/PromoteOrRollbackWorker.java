package canarydeployment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PromoteOrRollbackWorker implements Worker {

    @Override public String getTaskDefName() { return "cd_promote_or_rollback"; }

    @Override
    public TaskResult execute(Task task) {
        double errorRate = 0.0;
        Object erObj = task.getInputData().get("errorRate");
        if (erObj instanceof Number) errorRate = ((Number) erObj).doubleValue();
        else if (erObj instanceof String) { try { errorRate = Double.parseDouble((String) erObj); } catch (NumberFormatException ignored) {} }

        double threshold = 1.0;
        Object thObj = task.getInputData().get("threshold");
        if (thObj instanceof Number) threshold = ((Number) thObj).doubleValue();
        else if (thObj instanceof String) { try { threshold = Double.parseDouble((String) thObj); } catch (NumberFormatException ignored) {} }

        boolean promote = errorRate < threshold;
        String action = promote ? "promote" : "rollback";

        System.out.println("  [decision] " + action.toUpperCase() + " (error: " + errorRate + "%)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("action", action);
        result.getOutputData().put("errorRate", errorRate);
        result.getOutputData().put("threshold", threshold);
        return result;
    }
}
