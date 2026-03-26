package dataanonymization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Suppresses direct-identifier fields by replacing values with [REDACTED].
 * Input: generalizedData, anonymizationLevel
 * Output: suppressed, suppressedCount
 */
public class SuppressFieldsWorker implements Worker {

    private static final List<String> SUPPRESSED_FIELDS = List.of("email", "ssn", "phone");

    @Override
    public String getTaskDefName() {
        return "an_suppress_fields";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) task.getInputData().get("generalizedData");
        if (data == null) {
            data = List.of();
        }

        List<Map<String, Object>> suppressed = new ArrayList<>();
        for (Map<String, Object> record : data) {
            Map<String, Object> r = new LinkedHashMap<>(record);
            for (String field : SUPPRESSED_FIELDS) {
                if (r.containsKey(field) && r.get(field) != null) {
                    r.put(field, "[REDACTED]");
                }
            }
            suppressed.add(r);
        }

        System.out.println("  [suppress] Suppressed " + SUPPRESSED_FIELDS.size() + " fields: " + String.join(", ", SUPPRESSED_FIELDS));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("suppressed", suppressed);
        result.getOutputData().put("suppressedCount", SUPPRESSED_FIELDS.size());
        return result;
    }
}
