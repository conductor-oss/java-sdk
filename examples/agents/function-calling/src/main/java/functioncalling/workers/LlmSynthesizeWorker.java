package functioncalling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * LLM synthesis worker — takes the function execution result and produces
 * a natural-language answer for the user.
 */
public class LlmSynthesizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fc_llm_synthesize";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String userQuery = (String) task.getInputData().get("userQuery");
        String functionName = (String) task.getInputData().get("functionName");
        Map<String, Object> functionResult = (Map<String, Object>) task.getInputData().get("functionResult");

        if (userQuery == null || userQuery.isBlank()) {
            userQuery = "general query";
        }
        if (functionName == null || functionName.isBlank()) {
            functionName = "unknown";
        }

        System.out.println("  [fc_llm_synthesize] Synthesizing answer for query: " + userQuery);

        String answer;
        if (functionResult != null && functionResult.containsKey("price")) {
            Object ticker = functionResult.getOrDefault("ticker", "the stock");
            Object companyName = functionResult.getOrDefault("companyName", "the company");
            Object price = functionResult.get("price");
            Object currency = functionResult.getOrDefault("currency", "USD");
            Object change = functionResult.get("change");
            Object changePercent = functionResult.get("changePercent");

            answer = String.format(
                    "The current price of %s (%s) is %s %s. "
                    + "The stock is up %s (%s%%) today. "
                    + "This data was retrieved using the %s function.",
                    companyName, ticker, currency, price,
                    change, changePercent, functionName);
        } else {
            answer = "I was unable to retrieve the requested information for your query: " + userQuery;
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("confidence", 0.97);
        result.getOutputData().put("sourceFunctionUsed", functionName);
        return result;
    }
}
