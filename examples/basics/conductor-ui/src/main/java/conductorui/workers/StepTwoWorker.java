package conductorui.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Step Two — Enriches data from step one with metadata.
 *
 * Input:  previousResult
 * Output: result, metadata (enriched, score)
 */
public class StepTwoWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ui_step_two";
    }

    @Override
    public TaskResult execute(Task task) {
        String previousResult = (String) task.getInputData().get("previousResult");

        if (previousResult == null || previousResult.isBlank()) {
            previousResult = "no previous data";
        }

        String result = "Enriched: " + previousResult;

        // Compute a relevance score based on input richness (word count)
        int wordCount = previousResult.trim().split("\\s+").length;
        double score = Math.min(1.0, wordCount * 0.1);
        score = Math.round(score * 100.0) / 100.0;

        System.out.println("  [ui_step_two] " + result + " (score=" + score + ")");

        TaskResult taskResult = new TaskResult(task);
        taskResult.setStatus(TaskResult.Status.COMPLETED);
        taskResult.getOutputData().put("result", result);
        taskResult.getOutputData().put("enriched", true);
        taskResult.getOutputData().put("score", score);
        return taskResult;
    }
}
