package datapartitioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Merges results from both partitions into a single combined output.
 *
 * Input:  resultA (list), resultB (list), countA (int), countB (int)
 * Output: mergedCount (int), summary (string), records (list)
 */
public class MergeResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "par_merge_results";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultA =
                (List<Map<String, Object>>) task.getInputData().get("resultA");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultB =
                (List<Map<String, Object>>) task.getInputData().get("resultB");

        if (resultA == null) {
            resultA = List.of();
        }
        if (resultB == null) {
            resultB = List.of();
        }

        int countA = toInt(task.getInputData().get("countA"), resultA.size());
        int countB = toInt(task.getInputData().get("countB"), resultB.size());

        List<Map<String, Object>> merged = new ArrayList<>(resultA);
        merged.addAll(resultB);

        int mergedCount = merged.size();
        String summary = "Merged " + countA + " + " + countB + " = " + mergedCount
                + " records from 2 partitions";

        System.out.println("  [par_merge_results] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("mergedCount", mergedCount);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("records", merged);
        return result;
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return fallback;
    }
}
