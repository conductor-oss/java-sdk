package dataaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups records by a specified dimension field.
 * Input: records (list of maps), groupBy (string field name)
 * Output: groups (map of key to list of records), groupCount
 */
public class GroupByDimensionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agg_group_by_dimension";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        String groupBy = (String) task.getInputData().get("groupBy");
        if (groupBy == null) {
            groupBy = "group";
        }

        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> record : records) {
            Object keyObj = record.get(groupBy);
            String key = keyObj != null ? String.valueOf(keyObj) : "unknown";
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }

        System.out.println("  [agg_group_by_dimension] Grouped into " + groups.size() + " groups by '" + groupBy + "'");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("groups", groups);
        result.getOutputData().put("groupCount", groups.size());
        return result;
    }
}
