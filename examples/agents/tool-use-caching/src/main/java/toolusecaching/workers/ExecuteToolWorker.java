package toolusecaching.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executes the requested tool and returns its result.
 *
 * Performs a currency conversion: converts an amount from one currency to another.
 * Input fields: toolName, toolArgs.
 * Output fields: result (map with conversion details), executionTimeMs, apiCallMade.
 */
public class ExecuteToolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "uc_execute_tool";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        Map<String, Object> toolArgs = (Map<String, Object>) task.getInputData().get("toolArgs");
        if (toolArgs == null) {
            toolArgs = Map.of();
        }

        System.out.println("  [uc_execute_tool] Executing tool: " + toolName);

        String from = toolArgs.get("from") != null ? toolArgs.get("from").toString() : "USD";
        String to = toolArgs.get("to") != null ? toolArgs.get("to").toString() : "EUR";

        double amount = 1000.0;
        Object amountObj = toolArgs.get("amount");
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).doubleValue();
        }

        double rate = 0.92;
        double convertedAmount = amount * rate;

        Map<String, Object> conversionResult = new LinkedHashMap<>();
        conversionResult.put("baseCurrency", from);
        conversionResult.put("targetCurrency", to);
        conversionResult.put("rate", rate);
        conversionResult.put("amount", amount);
        conversionResult.put("convertedAmount", convertedAmount);
        conversionResult.put("provider", "exchange_rates_api");
        conversionResult.put("timestamp", "2026-03-08T10:00:00Z");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", conversionResult);
        result.getOutputData().put("executionTimeMs", 312);
        result.getOutputData().put("apiCallMade", true);
        return result;
    }
}
