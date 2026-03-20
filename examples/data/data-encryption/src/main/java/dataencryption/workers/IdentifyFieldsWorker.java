package dataencryption.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Identifies which sensitive fields exist in the records.
 * Input: records (list), sensitiveFields (list)
 * Output: records (pass-through), fieldsToEncrypt (list)
 */
public class IdentifyFieldsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dn_identify_fields";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }
        List<String> sensitiveFields = (List<String>) task.getInputData().get("sensitiveFields");
        if (sensitiveFields == null) {
            sensitiveFields = List.of("ssn", "creditCard");
        }

        Set<String> found = new HashSet<>();
        for (Map<String, Object> record : records) {
            for (String field : sensitiveFields) {
                if (record.containsKey(field)) {
                    found.add(field);
                }
            }
        }

        List<String> fieldsToEncrypt = new ArrayList<>(found);
        System.out.println("  [identify] Found " + fieldsToEncrypt.size() + " sensitive fields to encrypt: [" + String.join(", ", fieldsToEncrypt) + "]");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("fieldsToEncrypt", fieldsToEncrypt);
        return result;
    }
}
