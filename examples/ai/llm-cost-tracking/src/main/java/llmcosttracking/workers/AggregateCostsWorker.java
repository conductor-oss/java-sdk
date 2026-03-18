package llmcosttracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker 4: Aggregates costs from all three LLM calls.
 * Takes gpt4Usage, claudeUsage, geminiUsage and calculates per-model
 * and total costs using fixed pricing constants.
 */
public class AggregateCostsWorker implements Worker {

    // Pricing per 1K tokens: [inputPrice, outputPrice]
    private static final Map<String, double[]> PRICING = Map.of(
            "gpt-4", new double[]{0.03, 0.06},
            "claude-3", new double[]{0.015, 0.075},
            "gemini", new double[]{0.0005, 0.0015}
    );

    @Override
    public String getTaskDefName() {
        return "ct_aggregate_costs";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> gpt4Usage = (Map<String, Object>) task.getInputData().get("gpt4Usage");
        Map<String, Object> claudeUsage = (Map<String, Object>) task.getInputData().get("claudeUsage");
        Map<String, Object> geminiUsage = (Map<String, Object>) task.getInputData().get("geminiUsage");

        List<Map<String, Object>> breakdown = new ArrayList<>();
        double totalCost = 0;
        int totalTokens = 0;

        for (Map<String, Object> usage : List.of(gpt4Usage, claudeUsage, geminiUsage)) {
            String model = (String) usage.get("model");
            int inputTokens = toInt(usage.get("inputTokens"));
            int outputTokens = toInt(usage.get("outputTokens"));

            double[] prices = PRICING.get(model);
            double cost = (inputTokens / 1000.0) * prices[0] + (outputTokens / 1000.0) * prices[1];

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("model", model);
            entry.put("inputTokens", inputTokens);
            entry.put("outputTokens", outputTokens);
            entry.put("cost", String.format("$%.4f", cost));

            breakdown.add(entry);
            totalCost += cost;
            totalTokens += inputTokens + outputTokens;
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("breakdown", breakdown);
        result.getOutputData().put("totalCost", String.format("$%.4f", totalCost));
        result.getOutputData().put("totalTokens", totalTokens);
        return result;
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
