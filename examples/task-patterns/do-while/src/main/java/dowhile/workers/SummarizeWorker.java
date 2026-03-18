package dowhile.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Summarizes the results after the DO_WHILE loop completes.
 * Takes the total number of iterations and produces a summary.
 */
public class SummarizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dw_summarize";
    }

    @Override
    public TaskResult execute(Task task) {
        Object totalIterationsObj = task.getInputData().get("totalIterations");
        int totalIterations;
        if (totalIterationsObj instanceof Number) {
            totalIterations = ((Number) totalIterationsObj).intValue();
        } else {
            totalIterations = 0;
        }

        System.out.println("  [dw_summarize] Summarizing " + totalIterations + " processed items");

        String summary = "Processed " + totalIterations + " items successfully";

        TaskResult taskResult = new TaskResult(task);
        taskResult.setStatus(TaskResult.Status.COMPLETED);
        taskResult.getOutputData().put("totalProcessed", totalIterations);
        taskResult.getOutputData().put("summary", summary);
        return taskResult;
    }
}
