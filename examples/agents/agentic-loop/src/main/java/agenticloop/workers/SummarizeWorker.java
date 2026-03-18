package agenticloop.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Summarizes the agentic loop execution after all iterations are complete.
 * Input:  {goal, totalIterations}
 * Output: {summary:"Achieved goal ... through N think-act-observe cycles"}
 */
public class SummarizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "al_summarize";
    }

    @Override
    public TaskResult execute(Task task) {
        String goal = (String) task.getInputData().get("goal");
        if (goal == null || goal.isBlank()) {
            goal = "No goal specified";
        }

        int totalIterations = toInt(task.getInputData().get("totalIterations"), 3);

        String summary = "Achieved goal '" + goal + "' through "
                + totalIterations + " think-act-observe cycles";

        System.out.println("  [al_summarize] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
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
