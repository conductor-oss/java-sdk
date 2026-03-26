package externalpayload.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes the summary from the generate step.
 *
 * In a real system, this worker could also fetch the full data from the
 * storage reference. Here it processes just the summary to produce a result.
 *
 * Input:  { summary: { recordCount, avgSize, totalBytes } }
 * Output: { result: "Processed N records" }
 */
public class ProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ep_process";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> summary =
                (Map<String, Object>) task.getInputData().get("summary");

        int recordCount;
        if (summary != null && summary.get("recordCount") instanceof Number) {
            recordCount = ((Number) summary.get("recordCount")).intValue();
        } else {
            recordCount = 0;
        }

        System.out.println("  [ep_process] Processing summary for " + recordCount + " records");

        String processedResult = "Processed " + recordCount + " records";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", processedResult);
        return result;
    }
}
