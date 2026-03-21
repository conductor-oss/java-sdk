package schemaevolution.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies schema transforms to sample data.
 * Input: transforms, sampleData
 * Output: transformedData, recordCount
 */
public class ApplyTransformWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sh_apply_transform";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) task.getInputData().get("sampleData");
        if (data == null) {
            data = List.of();
        }

        List<Map<String, Object>> transformed = new ArrayList<>();
        for (Map<String, Object> record : data) {
            Map<String, Object> r = new LinkedHashMap<>(record);
            // ADD_FIELD: middle_name
            r.put("middle_name", null);
            // RENAME_FIELD: phone -> phone_number
            Object phone = r.remove("phone");
            r.put("phone_number", phone);
            // CHANGE_TYPE: age string -> integer
            Object ageVal = r.get("age");
            if (ageVal instanceof String) {
                try {
                    r.put("age", Integer.parseInt((String) ageVal));
                } catch (NumberFormatException e) {
                    r.put("age", 0);
                }
            }
            // DROP_FIELD: legacy_id
            r.remove("legacy_id");
            // ADD_DEFAULT: status = "active"
            if (!r.containsKey("status") || r.get("status") == null) {
                r.put("status", "active");
            }
            transformed.add(r);
        }

        System.out.println("  [apply] Transformed " + transformed.size() + " records with 5 schema changes");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformedData", transformed);
        result.getOutputData().put("recordCount", transformed.size());
        return result;
    }
}
