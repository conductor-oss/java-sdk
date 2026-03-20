package datavalidation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks that required fields (name, email, age) are present and non-empty.
 * Input: records, schema
 * Output: passedRecords, passedCount, errorCount, errors
 */
public class CheckRequiredWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vd_check_required";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        List<String> requiredFields = List.of("name", "email", "age");
        List<Map<String, Object>> passed = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            List<String> missing = new ArrayList<>();
            for (String field : requiredFields) {
                Object value = record.get(field);
                if (value == null) {
                    missing.add(field);
                } else if (value instanceof String s && s.isEmpty()) {
                    missing.add(field);
                }
            }
            if (!missing.isEmpty()) {
                errors.add(Map.of("record", i, "missing", missing));
            } else {
                passed.add(record);
            }
        }

        System.out.println("  [vd_check_required] " + passed.size() + " passed, " + errors.size() + " failed required-field check");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("passedRecords", passed);
        result.getOutputData().put("passedCount", passed.size());
        result.getOutputData().put("errorCount", errors.size());
        result.getOutputData().put("errors", errors);
        return result;
    }
}
