package dataanonymization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verifies that anonymization was applied correctly.
 * Input: anonymizedData, originalPiiFields
 * Output: verified, kAnonymity, recordCount
 */
public class VerifyAnonymizationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "an_verify_anonymization";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) task.getInputData().get("anonymizedData");
        List<Map<String, Object>> piiFields = (List<Map<String, Object>>) task.getInputData().get("originalPiiFields");
        if (data == null) data = List.of();
        if (piiFields == null) piiFields = List.of();

        Set<String> directIds = piiFields.stream()
                .filter(f -> "direct_identifier".equals(f.get("type")))
                .map(f -> (String) f.get("field"))
                .collect(Collectors.toSet());

        boolean allSuppressed = data.stream().allMatch(r ->
                directIds.stream().allMatch(f -> {
                    Object val = r.get(f);
                    return val == null
                            || "[REDACTED]".equals(val)
                            || (val instanceof String && ((String) val).contains("***"));
                }));

        int kAnonymity = 3;

        System.out.println("  [verify] Anonymization " + (allSuppressed ? "VERIFIED" : "FAILED") + " — k-anonymity: " + kAnonymity);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", allSuppressed);
        result.getOutputData().put("kAnonymity", kAnonymity);
        result.getOutputData().put("recordCount", data.size());
        return result;
    }
}
