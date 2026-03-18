package batchprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Summarizes batch processing results.
 * Input: totalBatches, totalRecords, iterations
 * Output: summary
 */
public class SummarizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bp_summarize";
    }

    @Override
    public TaskResult execute(Task task) {
        Object totalRecordsObj = task.getInputData().get("totalRecords");
        int totalRecords = 0;
        if (totalRecordsObj instanceof Number) {
            totalRecords = ((Number) totalRecordsObj).intValue();
        }

        Object iterationsObj = task.getInputData().get("iterations");
        int iterations = 0;
        if (iterationsObj instanceof Number) {
            iterations = ((Number) iterationsObj).intValue();
        }

        String summary = "Batch processing complete: " + totalRecords + " records in " + iterations + " batches";

        System.out.println("  [summary] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
