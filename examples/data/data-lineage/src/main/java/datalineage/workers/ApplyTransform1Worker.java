package datalineage.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies transform 1: uppercase names and tracks lineage.
 * Input: records (list), lineage (list)
 * Output: records (list with transformed names), lineage (list with new entry)
 */
public class ApplyTransform1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ln_apply_transform_1";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }
        List<Map<String, Object>> prevLineage = (List<Map<String, Object>>) task.getInputData().get("lineage");
        if (prevLineage == null) {
            prevLineage = List.of();
        }

        List<Map<String, Object>> transformed = new ArrayList<>();
        for (Map<String, Object> record : records) {
            Map<String, Object> copy = new HashMap<>(record);
            String name = copy.get("name") != null ? copy.get("name").toString().toUpperCase() : "";
            copy.put("name", name);
            copy.put("transformed_1", true);
            transformed.add(copy);
        }

        List<Map<String, Object>> lineage = new ArrayList<>(prevLineage);
        lineage.add(Map.of(
                "step", "transform_1",
                "name", "uppercase_names",
                "timestamp", Instant.now().toString(),
                "recordCount", transformed.size(),
                "operation", "transform",
                "fieldsModified", List.of("name")
        ));

        System.out.println("  [transform-1] Applied uppercase_names to " + transformed.size() + " records");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", transformed);
        result.getOutputData().put("lineage", lineage);
        return result;
    }
}
