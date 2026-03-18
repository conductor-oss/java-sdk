package csvprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transforms validated rows: normalizes names, lowercases emails,
 * uppercases departments, parses salaries to doubles.
 * Input: validRows (list of maps), headers (list)
 * Output: rows (transformed list), count
 */
public class TransformFieldsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cv_transform_fields";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> validRows =
                (List<Map<String, Object>>) task.getInputData().get("validRows");
        if (validRows == null) {
            validRows = List.of();
        }

        List<Map<String, Object>> transformed = new ArrayList<>();

        for (Map<String, Object> r : validRows) {
            String name = r.get("name") != null ? r.get("name").toString() : "";
            String email = r.get("email") != null ? r.get("email").toString() : "";
            String dept = r.get("dept") != null ? r.get("dept").toString() : "unknown";
            String salaryStr = r.get("salary") != null ? r.get("salary").toString() : "0";

            double salary;
            try {
                salary = Double.parseDouble(salaryStr);
            } catch (NumberFormatException e) {
                salary = 0.0;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("fullName", name.trim());
            row.put("emailAddress", email.toLowerCase().trim());
            row.put("department", dept.toUpperCase());
            row.put("salary", salary);
            transformed.add(row);
        }

        System.out.println("  [transform] Transformed " + transformed.size()
                + " rows (normalized names, emails, departments)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rows", transformed);
        result.getOutputData().put("count", transformed.size());
        return result;
    }
}
