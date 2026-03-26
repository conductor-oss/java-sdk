package optionaltasks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for opt_summarize — summarizes the workflow results.
 *
 * Checks whether the enrichment data is available. If enrichment was
 * provided (the optional task succeeded), includes it in the summary.
 * If enrichment is missing (the optional task failed or was skipped),
 * produces a summary noting the missing enrichment.
 *
 * Returns { summary: "..." } where the summary string varies depending
 * on whether enrichment data was present.
 */
public class SummarizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "opt_summarize";
    }

    @Override
    public TaskResult execute(Task task) {
        Object processedData = task.getInputData().get("processedData");
        Object enrichedData = task.getInputData().get("enrichedData");

        System.out.println("  [opt_summarize] processedData=" + processedData + " enrichedData=" + enrichedData);

        String summary;
        if (enrichedData != null && !enrichedData.toString().isEmpty()) {
            summary = "Processed: " + processedData + ", Enriched: " + enrichedData;
        } else {
            summary = "Processed: " + processedData + ", Enrichment: skipped";
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
