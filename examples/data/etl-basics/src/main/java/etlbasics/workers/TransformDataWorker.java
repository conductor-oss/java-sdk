package etlbasics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transforms raw records: trims names, lowercases emails, parses amount strings to doubles.
 * Input: rawRecords, rules
 * Output: records (transformed), transformedCount
 */
public class TransformDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "el_transform_data";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> rawRecords =
                (List<Map<String, Object>>) task.getInputData().get("rawRecords");
        if (rawRecords == null) {
            rawRecords = List.of();
        }

        System.out.println("  [el_transform_data] Transforming " + rawRecords.size() + " records");

        List<Map<String, Object>> transformed = new ArrayList<>();
        for (Map<String, Object> raw : rawRecords) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", raw.get("id"));

            Object nameObj = raw.get("name");
            String name = nameObj != null ? nameObj.toString().trim() : "";
            record.put("name", name);

            Object emailObj = raw.get("email");
            String email = emailObj != null ? emailObj.toString().toLowerCase() : "";
            record.put("email", email);

            Object amountObj = raw.get("amount");
            double amount = 0.0;
            if (amountObj != null) {
                try {
                    amount = Double.parseDouble(amountObj.toString());
                } catch (NumberFormatException e) {
                    amount = 0.0;
                }
            }
            record.put("amount", amount);

            transformed.add(record);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", transformed);
        result.getOutputData().put("transformedCount", transformed.size());
        return result;
    }
}
