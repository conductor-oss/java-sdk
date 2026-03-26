package featureengineering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Transforms features by adding derived features (log, polynomial, ratio).
 * Input: features
 * Output: transformed, transformedCount
 */
public class TransformFeaturesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fe_transform_features";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> features = (List<Map<String, Object>>) task.getInputData().get("features");
        if (features == null) {
            features = List.of();
        }

        List<Map<String, Object>> transformed = new ArrayList<>();
        for (Map<String, Object> f : features) {
            Map<String, Object> t = new LinkedHashMap<>(f);
            double income = toDouble(f.get("income"));
            int age = toInt(f.get("age"));
            int numProducts = toInt(f.get("num_products"));
            int tenureMonths = toInt(f.get("tenure_months"));
            double balance = toDouble(f.get("balance"));

            t.put("log_income", Math.log(income + 1));
            t.put("age_squared", age * age);
            t.put("income_per_product", numProducts > 0 ? income / numProducts : 0.0);
            t.put("tenure_years", Math.round((tenureMonths / 12.0) * 100.0) / 100.0);
            t.put("balance_to_income_ratio", income > 0 ? Math.round((balance / income) * 10000.0) / 10000.0 : 0.0);
            transformed.add(t);
        }

        int transformedCount = transformed.isEmpty() ? 0 : transformed.get(0).size();

        System.out.println("  [transform] Added 5 derived features (log, polynomial, ratio)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformed", transformed);
        result.getOutputData().put("transformedCount", transformedCount);
        return result;
    }

    private double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return 0.0;
    }

    private int toInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        return 0;
    }
}
