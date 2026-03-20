package datadedup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups keyed records by dedupKey and identifies duplicate groups.
 * Input: keyedRecords (list with dedupKey field)
 * Output: groups (list of lists), groupCount (number of groups with >1 record),
 *         duplicateCount (total extra records across all duplicate groups)
 */
public class FindDuplicatesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dp_find_duplicates";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> keyedRecords =
                (List<Map<String, Object>>) task.getInputData().get("keyedRecords");
        if (keyedRecords == null) {
            keyedRecords = List.of();
        }

        // Group by dedupKey preserving insertion order
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> record : keyedRecords) {
            String key = (String) record.get("dedupKey");
            if (key == null) {
                key = "";
            }
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }

        List<List<Map<String, Object>>> groups = new ArrayList<>(grouped.values());

        int dupGroupCount = 0;
        int dupCount = 0;
        for (List<Map<String, Object>> group : groups) {
            if (group.size() > 1) {
                dupGroupCount++;
                dupCount += group.size() - 1;
            }
        }

        System.out.println("  [dp_find_duplicates] Found " + dupGroupCount
                + " duplicate groups (" + dupCount + " extra records) out of "
                + groups.size() + " unique keys");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("groups", groups);
        result.getOutputData().put("groupCount", dupGroupCount);
        result.getOutputData().put("duplicateCount", dupCount);
        return result;
    }
}
