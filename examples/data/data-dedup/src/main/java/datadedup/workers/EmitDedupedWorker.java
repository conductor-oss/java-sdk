package datadedup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Emits the final deduplicated result with a summary.
 * Input: deduped (list), originalCount (int), dupCount (int)
 * Output: dedupedCount, summary
 */
public class EmitDedupedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dp_emit_deduped";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> deduped =
                (List<Map<String, Object>>) task.getInputData().get("deduped");
        if (deduped == null) {
            deduped = List.of();
        }

        Object originalCountObj = task.getInputData().get("originalCount");
        int originalCount = 0;
        if (originalCountObj instanceof Number) {
            originalCount = ((Number) originalCountObj).intValue();
        }

        Object dupCountObj = task.getInputData().get("dupCount");
        int dupCount = 0;
        if (dupCountObj instanceof Number) {
            dupCount = ((Number) dupCountObj).intValue();
        }

        int dedupedCount = deduped.size();
        String summary = "Dedup complete: " + originalCount + " \u2192 " + dedupedCount
                + " records (" + dupCount + " duplicates removed)";

        System.out.println("  [dp_emit_deduped] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dedupedCount", dedupedCount);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
