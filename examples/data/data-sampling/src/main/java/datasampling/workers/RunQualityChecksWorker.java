package datasampling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs quality checks on the sampled data.
 * Input: sample (list of records), threshold (double)
 * Output: qualityScore (double), decision ("pass"/"fail"), issues (list of strings),
 *         validCount (int), checkedCount (int)
 *
 * Checks each record for:
 *   - non-empty "name" field (must be a non-null, non-empty string)
 *   - numeric "value" field >= 0
 *
 * qualityScore = validCount / checkedCount (0.0 if no records)
 * decision = "pass" if qualityScore >= threshold, else "fail"
 */
public class RunQualityChecksWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sm_run_quality_checks";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> sample = (List<Map<String, Object>>) task.getInputData().get("sample");
        if (sample == null) {
            sample = List.of();
        }

        double threshold = toDouble(task.getInputData().get("threshold"), 0.8);

        System.out.println("  [sm_run_quality_checks] Checking " + sample.size() + " records with threshold " + threshold);

        int validCount = 0;
        int checkedCount = sample.size();
        List<String> issues = new ArrayList<>();

        for (int i = 0; i < sample.size(); i++) {
            Map<String, Object> record = sample.get(i);
            boolean valid = true;

            // Check non-empty name
            Object nameObj = record.get("name");
            if (nameObj == null || nameObj.toString().isEmpty()) {
                issues.add("Record " + i + ": missing or empty name");
                valid = false;
            }

            // Check numeric value >= 0
            Object valueObj = record.get("value");
            if (valueObj == null) {
                issues.add("Record " + i + ": missing value");
                valid = false;
            } else if (valueObj instanceof Number) {
                if (((Number) valueObj).doubleValue() < 0) {
                    issues.add("Record " + i + ": negative value " + valueObj);
                    valid = false;
                }
            } else {
                try {
                    double val = Double.parseDouble(valueObj.toString());
                    if (val < 0) {
                        issues.add("Record " + i + ": negative value " + val);
                        valid = false;
                    }
                } catch (NumberFormatException e) {
                    issues.add("Record " + i + ": non-numeric value '" + valueObj + "'");
                    valid = false;
                }
            }

            if (valid) {
                validCount++;
            }
        }

        double qualityScore = checkedCount > 0 ? (double) validCount / checkedCount : 0.0;
        String decision = qualityScore >= threshold ? "pass" : "fail";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("qualityScore", qualityScore);
        result.getOutputData().put("decision", decision);
        result.getOutputData().put("issues", issues);
        result.getOutputData().put("validCount", validCount);
        result.getOutputData().put("checkedCount", checkedCount);
        return result;
    }

    private double toDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
