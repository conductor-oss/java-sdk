package etlbasics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates transformed records: filters for records with non-empty name, email, and amount > 0.
 * Input: transformedRecords
 * Output: validRecords, validCount, invalidCount
 */
public class ValidateOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "el_validate_output";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> transformedRecords =
                (List<Map<String, Object>>) task.getInputData().get("transformedRecords");
        if (transformedRecords == null) {
            transformedRecords = List.of();
        }

        System.out.println("  [el_validate_output] Validating " + transformedRecords.size() + " records");

        List<Map<String, Object>> validRecords = new ArrayList<>();
        int invalidCount = 0;

        for (Map<String, Object> record : transformedRecords) {
            Object nameObj = record.get("name");
            String name = nameObj != null ? nameObj.toString() : "";

            Object emailObj = record.get("email");
            String email = emailObj != null ? emailObj.toString() : "";

            Object amountObj = record.get("amount");
            double amount = 0.0;
            if (amountObj instanceof Number) {
                amount = ((Number) amountObj).doubleValue();
            }

            if (!name.isEmpty() && !email.isEmpty() && amount > 0) {
                validRecords.add(record);
            } else {
                invalidCount++;
            }
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validRecords", validRecords);
        result.getOutputData().put("validCount", validRecords.size());
        result.getOutputData().put("invalidCount", invalidCount);
        return result;
    }
}
