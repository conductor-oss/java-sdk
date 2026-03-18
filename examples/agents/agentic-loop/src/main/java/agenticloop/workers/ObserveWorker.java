package agenticloop.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Observes the outcome of the action and assesses goal progress.
 * Input:  {actionResult, iteration}
 * Output: {observation, goalProgress:"advancing"}
 */
public class ObserveWorker implements Worker {

    private static final String[] OBSERVATIONS = {
        "Information gathering phase complete; sufficient data collected to proceed with analysis",
        "Pattern analysis reveals clear structure; ready to formulate recommendations",
        "Synthesis complete; goal has been fully addressed with comprehensive recommendations"
    };

    @Override
    public String getTaskDefName() {
        return "al_observe";
    }

    @Override
    public TaskResult execute(Task task) {
        String actionResult = (String) task.getInputData().get("actionResult");
        if (actionResult == null || actionResult.isBlank()) {
            actionResult = "No action result provided";
        }

        int iteration = toInt(task.getInputData().get("iteration"), 1);

        int obsIndex = (iteration - 1) % OBSERVATIONS.length;
        if (obsIndex < 0) obsIndex = 0;
        String observation = OBSERVATIONS[obsIndex];

        System.out.println("  [al_observe] Iteration " + iteration + ": " + observation);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("observation", observation);
        result.getOutputData().put("goalProgress", "advancing");
        return result;
    }

    private int toInt(Object value, int defaultVal) {
        if (value == null) return defaultVal;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
