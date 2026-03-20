package datamasking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Detects PII fields (SSN, email, phone) in records.
 * Input: records (list), policy
 * Output: records (pass-through), piiFields (list), piiFieldCount
 */
public class DetectPiiWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mk_detect_pii";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("records", List.of());

        Set<String> piiFields = new LinkedHashSet<>();
        for (Map<String, Object> r : records) {
            if (r.containsKey("ssn") && r.get("ssn") != null) piiFields.add("ssn");
            if (r.containsKey("email") && r.get("email") != null) piiFields.add("email");
            if (r.containsKey("phone") && r.get("phone") != null) piiFields.add("phone");
        }

        List<String> fieldList = new ArrayList<>(piiFields);
        System.out.println("  [detect] Detected " + fieldList.size() + " PII fields: ["
                + String.join(", ", fieldList) + "]");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("piiFields", fieldList);
        result.getOutputData().put("piiFieldCount", fieldList.size());
        return result;
    }
}
