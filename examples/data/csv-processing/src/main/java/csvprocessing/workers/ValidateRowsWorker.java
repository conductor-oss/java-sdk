package csvprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates parsed CSV rows: each row must have a non-empty name and an email containing "@".
 * Input: rows (list of maps), headers (list)
 * Output: validRows, validCount, invalidCount, errors
 */
public class ValidateRowsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cv_validate_rows";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> rows = (List<Map<String, Object>>) task.getInputData().get("rows");
        if (rows == null) {
            rows = List.of();
        }

        List<Map<String, Object>> validRows = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String name = row.get("name") != null ? row.get("name").toString() : "";
            String email = row.get("email") != null ? row.get("email").toString() : "";

            if (!name.isEmpty() && email.contains("@")) {
                validRows.add(row);
            } else {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("row", i + 1);
                error.put("reason", name.isEmpty() ? "missing name" : "invalid email");
                errors.add(error);
            }
        }

        System.out.println("  [validate] " + validRows.size() + " valid, " + errors.size() + " invalid rows");
        if (!errors.isEmpty()) {
            System.out.println("    Invalid: " + errors);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validRows", validRows);
        result.getOutputData().put("validCount", validRows.size());
        result.getOutputData().put("invalidCount", errors.size());
        result.getOutputData().put("errors", errors);
        return result;
    }
}
