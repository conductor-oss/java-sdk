package riskassessment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Analyzes market risk using volatility, beta, and correlation data.
 */
public class MarketRiskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rsk_market_risk";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("marketData");
        if (data == null) data = Map.of();

        double volatility = toDouble(data.get("volatility"));
        double beta = toDouble(data.get("beta"));
        int riskScore = (int) Math.round((volatility / 30 + beta / 2) * 50);

        System.out.println("  [market] Volatility: " + volatility + "%, Beta: " + beta + ", Risk: " + riskScore);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("riskScore", riskScore);
        result.getOutputData().put("var95", 2450000);
        result.getOutputData().put("stressTestLoss", 4200000);
        result.getOutputData().put("methodology", "Historical VaR");
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
