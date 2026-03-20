package eventbatching.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Processes a single batch of events by index.
 *
 * Input:  {batches: [[...], [...], ...], iteration: N}
 * Output: {batchIndex: iteration, eventsProcessed: batch.size()}
 *
 * The iteration value (0-based) selects which batch to process.
 */
public class ProcessBatchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eb_process_batch";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<List<Map<String, Object>>> batches =
                (List<List<Map<String, Object>>>) task.getInputData().get("batches");
        int iteration = 0;
        Object iterObj = task.getInputData().get("iteration");
        if (iterObj instanceof Number) {
            iteration = ((Number) iterObj).intValue();
        }

        int eventsProcessed = 0;
        if (batches != null && iteration < batches.size()) {
            eventsProcessed = batches.get(iteration).size();
        }

        System.out.println("  [eb_process_batch] Processed batch " + iteration
                + " with " + eventsProcessed + " events");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("batchIndex", iteration);
        result.getOutputData().put("eventsProcessed", eventsProcessed);
        return result;
    }
}
