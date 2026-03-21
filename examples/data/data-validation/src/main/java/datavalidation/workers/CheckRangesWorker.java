package datavalidation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks value ranges: age must be 0-150, email must contain "@".
 * Input: records, schema
 * Output: passedRecords, passedCount, errorCount, errors
 */
public class CheckRangesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vd_check_ranges";
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
            List<String> rangeIssues = new ArrayList<>();

            Object ageObj = record.get("age");
            if (ageObj instanceof Number num) {
                double age = num.doubleValue();
                if (age < 0 || age > 150) {
                    rangeIssues.add("age out of range (0-150)");
                }
            }

            Object emailObj = record.get("email");
            if (emailObj instanceof String email) {
                if (!email.contains("@")) {
                    rangeIssues.add("email format invalid");
                }
            }

            if (!rangeIssues.isEmpty()) {
                errors.add(Map.of("record", i, "issues", rangeIssues));
            } else {
                passed.add(record);
            }
        }

        System.out.println("  [vd_check_ranges] " + passed.size() + " passed, " + errors.size() + " failed range/format check");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("passedRecords", passed);
        result.getOutputData().put("passedCount", passed.size());
        result.getOutputData().put("errorCount", errors.size());
        result.getOutputData().put("errors", errors);
        return result;
    }
}
