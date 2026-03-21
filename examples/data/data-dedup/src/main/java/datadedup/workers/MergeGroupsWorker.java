package datadedup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges duplicate groups by picking the first record from each group and removing dedupKey.
 * Input: groups (list of lists)
 * Output: mergedRecords (list of cleaned records)
 */
public class MergeGroupsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dp_merge_groups";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<List<Map<String, Object>>> groups =
                (List<List<Map<String, Object>>>) task.getInputData().get("groups");
        if (groups == null) {
            groups = List.of();
        }

        List<Map<String, Object>> mergedRecords = new ArrayList<>();
        for (List<Map<String, Object>> group : groups) {
            if (group == null || group.isEmpty()) {
                continue;
            }
            // Pick first record from each group
            Map<String, Object> best = group.get(0);
            // Remove dedupKey
            Map<String, Object> clean = new LinkedHashMap<>(best);
            clean.remove("dedupKey");
            mergedRecords.add(clean);
        }

        System.out.println("  [dp_merge_groups] Merged " + groups.size()
                + " groups into " + mergedRecords.size() + " unique records");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("mergedRecords", mergedRecords);
        return result;
    }
}
