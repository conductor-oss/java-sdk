package datamigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates extracted records, filtering out invalid ones.
 * Input: records, schema
 * Output: validRecords, validCount, invalidCount
 */
public class ValidateSourceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mi_validate_source";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        List<Map<String, Object>> valid = new ArrayList<>();
        List<Map<String, Object>> invalid = new ArrayList<>();

        for (Map<String, Object> r : records) {
            String name = (String) r.get("name");
            String email = (String) r.get("email");
            if (name != null && email != null && email.contains("@") && email.contains(".")) {
                valid.add(r);
            } else {
                invalid.add(r);
            }
        }

        System.out.println("  [validate] " + valid.size() + " valid, " + invalid.size() + " invalid (missing name or bad email)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validRecords", valid);
        result.getOutputData().put("validCount", valid.size());
        result.getOutputData().put("invalidCount", invalid.size());
        return result;
    }
}
