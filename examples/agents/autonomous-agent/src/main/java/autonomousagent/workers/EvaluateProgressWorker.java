package autonomousagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Evaluates progress after each executed step.
 * Input:  {stepResult, iteration, goal}
 * Output: {progress, onTrack, assessment}
 */
public class EvaluateProgressWorker implements Worker {

    private static final String[] ASSESSMENTS = {
        "Step 1 completed successfully, 66% remaining",
        "Step 2 completed successfully, 33% remaining",
        "Step 3 completed successfully, 0% remaining"
    };

    private static final int[] PROGRESS = {33, 66, 100};

    @Override
    public String getTaskDefName() {
        return "aa_evaluate_progress";
    }

    @Override
    public TaskResult execute(Task task) {
        String stepResult = (String) task.getInputData().get("stepResult");
        if (stepResult == null || stepResult.isBlank()) {
            stepResult = "";
        }

        int iteration = toInt(task.getInputData().get("iteration"), 1);

        String goal = (String) task.getInputData().get("goal");
        if (goal == null || goal.isBlank()) {
            goal = "";
        }

        int index = (iteration - 1) % ASSESSMENTS.length;
        if (index < 0) index = 0;
        String assessment = ASSESSMENTS[index];
        int progress = PROGRESS[index];

        System.out.println("  [aa_evaluate_progress] Iteration " + iteration + ": " + assessment);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("progress", progress);
        result.getOutputData().put("onTrack", true);
        result.getOutputData().put("assessment", assessment);
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
