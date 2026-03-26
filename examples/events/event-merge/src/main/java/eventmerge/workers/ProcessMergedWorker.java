package eventmerge.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Processes the merged event list.
 * Input: mergedEvents (list), totalCount (int)
 * Output: status ("all_processed"), count (same as totalCount)
 */
public class ProcessMergedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mg_process_merged";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, String>> mergedEvents =
                (List<Map<String, String>>) task.getInputData().get("mergedEvents");
        if (mergedEvents == null) {
            mergedEvents = Collections.emptyList();
        }

        Object totalCountObj = task.getInputData().get("totalCount");
        int totalCount;
        if (totalCountObj instanceof Number) {
            totalCount = ((Number) totalCountObj).intValue();
        } else {
            totalCount = mergedEvents.size();
        }

        System.out.println("  [mg_process_merged] Processing " + totalCount + " merged events");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "all_processed");
        result.getOutputData().put("count", totalCount);
        return result;
    }
}
