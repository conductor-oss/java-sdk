package dataanonymization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Identifies PII fields in the dataset.
 * Input: dataset
 * Output: piiFields, piiFieldCount
 */
public class IdentifyPiiWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "an_identify_pii";
    }

    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> piiFields = List.of(
                Map.of("field", "name", "type", "direct_identifier", "action", "generalize"),
                Map.of("field", "email", "type", "direct_identifier", "action", "suppress"),
                Map.of("field", "ssn", "type", "direct_identifier", "action", "suppress"),
                Map.of("field", "age", "type", "quasi_identifier", "action", "generalize"),
                Map.of("field", "zipCode", "type", "quasi_identifier", "action", "generalize"),
                Map.of("field", "phone", "type", "direct_identifier", "action", "suppress")
        );

        String fieldNames = piiFields.stream().map(f -> (String) f.get("field")).collect(Collectors.joining(", "));
        System.out.println("  [identify] Found " + piiFields.size() + " PII fields: " + fieldNames);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("piiFields", piiFields);
        result.getOutputData().put("piiFieldCount", piiFields.size());
        return result;
    }
}
