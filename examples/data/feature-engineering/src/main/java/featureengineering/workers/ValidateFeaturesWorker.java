package featureengineering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Validates normalized features are in [0,1] range with no nulls.
 * Input: normalizedFeatures, featureConfig
 * Output: passed, allInRange, hasNulls, featureVector
 */
public class ValidateFeaturesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fe_validate_features";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> features = (List<Map<String, Object>>) task.getInputData().get("normalizedFeatures");
        if (features == null) {
            features = List.of();
        }

        boolean allInRange = true;
        boolean hasNulls = false;

        for (Map<String, Object> f : features) {
            for (Object v : f.values()) {
                if (v == null) {
                    hasNulls = true;
                } else if (v instanceof Number) {
                    double d = ((Number) v).doubleValue();
                    if (d < 0 || d > 1) {
                        allInRange = false;
                    }
                }
            }
        }

        boolean passed = allInRange && !hasNulls;
        List<String> featureVector = features.isEmpty() ? List.of() : new ArrayList<>(features.get(0).keySet());

        System.out.println("  [validate] " + (passed ? "PASSED" : "FAILED")
                + " — all in [0,1]: " + allInRange + ", no nulls: " + !hasNulls
                + ", vector size: " + featureVector.size());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("passed", passed);
        result.getOutputData().put("allInRange", allInRange);
        result.getOutputData().put("hasNulls", hasNulls);
        result.getOutputData().put("featureVector", featureVector);
        return result;
    }
}
