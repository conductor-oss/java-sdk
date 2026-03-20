package datamigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transforms records from old schema to new schema.
 * Input: validRecords, targetConfig
 * Output: transformed, transformedCount
 */
public class TransformSchemaWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mi_transform_schema";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("validRecords");
        if (records == null) {
            records = List.of();
        }

        List<Map<String, Object>> transformed = new ArrayList<>();
        for (Map<String, Object> r : records) {
            Map<String, Object> t = new LinkedHashMap<>();
            int id = ((Number) r.get("id")).intValue();
            t.put("employee_id", String.format("EMP-%05d", id));
            t.put("full_name", r.get("name"));
            String email = (String) r.get("email");
            t.put("email_address", email.replace("@old.com", "@new.com"));
            t.put("department_code", "DEPT-" + r.get("dept_id"));
            t.put("start_date", r.get("hire_date"));
            t.put("migrated_at", "2024-03-15T12:00:00Z");
            transformed.add(t);
        }

        System.out.println("  [transform] Transformed " + transformed.size() + " records to new schema (id format, email domain, dept code)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformed", transformed);
        result.getOutputData().put("transformedCount", transformed.size());
        return result;
    }
}
