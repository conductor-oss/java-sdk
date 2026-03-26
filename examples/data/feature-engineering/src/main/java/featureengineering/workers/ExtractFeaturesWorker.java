package featureengineering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Extracts raw features from input records.
 * Input: rawData, featureConfig
 * Output: features, featureCount
 */
public class ExtractFeaturesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fe_extract_features";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> rawData = (List<Map<String, Object>>) task.getInputData().get("rawData");
        if (rawData == null) {
            rawData = List.of();
        }

        List<Map<String, Object>> features = new ArrayList<>();
        for (Map<String, Object> record : rawData) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("age", record.get("age"));
            f.put("income", record.get("income"));
            f.put("tenure_months", record.get("tenure_months"));
            f.put("num_products", record.get("num_products"));
            f.put("has_credit_card", Boolean.TRUE.equals(record.get("has_credit_card")) ? 1 : 0);
            f.put("is_active", Boolean.TRUE.equals(record.get("is_active")) ? 1 : 0);
            f.put("balance", record.get("balance"));
            features.add(f);
        }

        int featureCount = features.isEmpty() ? 0 : features.get(0).size();

        System.out.println("  [extract] Extracted " + featureCount + " features from " + features.size() + " records");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("features", features);
        result.getOutputData().put("featureCount", featureCount);
        return result;
    }
}
