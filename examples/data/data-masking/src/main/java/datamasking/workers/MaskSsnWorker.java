package datamasking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Masks SSN fields in records (e.g. "123-45-6789" -> "***-**-6789").
 * Input: records (list), piiFields
 * Output: records (masked), maskedCount
 */
public class MaskSsnWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mk_mask_ssn";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("records", List.of());

        int maskedCount = 0;
        List<Map<String, Object>> masked = new ArrayList<>();
        for (Map<String, Object> r : records) {
            Map<String, Object> copy = new LinkedHashMap<>(r);
            if (copy.containsKey("ssn") && copy.get("ssn") != null) {
                String ssn = String.valueOf(copy.get("ssn"));
                String lastFour = ssn.length() >= 4 ? ssn.substring(ssn.length() - 4) : ssn;
                copy.put("ssn", "***-**-" + lastFour);
                maskedCount++;
            }
            masked.add(copy);
        }

        System.out.println("  [mask-ssn] Masked " + maskedCount + " SSN fields");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", masked);
        result.getOutputData().put("maskedCount", maskedCount);
        return result;
    }
}
