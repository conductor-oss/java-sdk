package datavalidation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks that field types are correct: name must be String, age must be Number.
 * Input: records, schema
 * Output: passedRecords, passedCount, errorCount, errors
 */
public class CheckTypesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vd_check_types";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        List<Map<String, Object>> passed = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            List<String> typeIssues = new ArrayList<>();
            if (!(record.get("name") instanceof String)) {
                typeIssues.add("name must be string");
            }
            if (!(record.get("age") instanceof Number)) {
                typeIssues.add("age must be number");
            }
            if (!typeIssues.isEmpty()) {
                errors.add(Map.of("record", i, "issues", typeIssues));
            } else {
                passed.add(record);
            }
        }

        System.out.println("  [vd_check_types] " + passed.size() + " passed, " + errors.size() + " failed type check");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("passedRecords", passed);
        result.getOutputData().put("passedCount", passed.size());
        result.getOutputData().put("errorCount", errors.size());
        result.getOutputData().put("errors", errors);
        return result;
    }
}
