package riskassessment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Analyzes credit risk using default rates and concentration data.
 */
public class CreditRiskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rsk_credit_risk";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("creditData");
        if (data == null) data = Map.of();

        double defaultRate = toDouble(data.get("defaultRate"));
        double concentration = toDouble(data.get("concentration"));
        int riskScore = (int) Math.round(defaultRate * 20 + concentration / 2);

        System.out.println("  [credit] Avg rating: " + data.get("avgRating") + ", Default rate: " + defaultRate + "%, Risk: " + riskScore);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("riskScore", riskScore);
        result.getOutputData().put("expectedLoss", 180000);
        result.getOutputData().put("unexpectedLoss", 520000);
        result.getOutputData().put("methodology", "CreditMetrics");
        return result;
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) {
            try { return Double.parseDouble((String) obj); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }
}
