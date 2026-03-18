package agenticloop.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Plans the next action based on the goal and current iteration.
 * Cycles through 3 fixed plan strings based on iteration number.
 * Input:  {goal, iteration}
 * Output: {plan, iteration}
 */
public class ThinkWorker implements Worker {

    private static final String[] PLANS = {
        "Research and gather information on the topic",
        "Analyze gathered data and identify key patterns",
        "Synthesize findings into actionable recommendations"
    };

    @Override
    public String getTaskDefName() {
        return "al_think";
    }

    @Override
    public TaskResult execute(Task task) {
        String goal = (String) task.getInputData().get("goal");
        if (goal == null || goal.isBlank()) {
            goal = "No goal specified";
        }

        int iteration = toInt(task.getInputData().get("iteration"), 1);

        // Cycle through plans (1-indexed iteration mapped to 0-indexed array)
        int planIndex = (iteration - 1) % PLANS.length;
        if (planIndex < 0) planIndex = 0;
        String plan = PLANS[planIndex];

        System.out.println("  [al_think] Iteration " + iteration + ": " + plan);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("plan", plan);
        result.getOutputData().put("iteration", iteration);
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
