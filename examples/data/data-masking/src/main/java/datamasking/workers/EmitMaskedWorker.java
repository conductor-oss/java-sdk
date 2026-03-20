package datamasking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Emits the final masked records and produces a summary.
 * Input: records (list), fieldsDetected, recordCount
 * Output: summary
 */
public class EmitMaskedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mk_emit_masked";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("records", List.of());
        Object fieldsDetected = task.getInputData().getOrDefault("fieldsDetected", 0);
        Object recordCount = task.getInputData().getOrDefault("recordCount", 0);

        String summary = "Masked " + fieldsDetected + " PII field types across " + recordCount + " records";
        System.out.println("  [emit] " + summary);
        for (Map<String, Object> r : records) {
            System.out.println("    " + r.getOrDefault("name", "?") + ": ssn=" + r.get("ssn")
                    + ", email=" + r.get("email") + ", phone=" + r.get("phone"));
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
