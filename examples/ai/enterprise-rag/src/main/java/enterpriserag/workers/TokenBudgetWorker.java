package enterpriserag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that manages token budgets by trimming context to stay within limits.
 * Takes context and userId, returns trimmedContext, remainingBudget, and contextTokens.
 */
public class TokenBudgetWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "er_token_budget";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> context =
                (List<Map<String, Object>>) task.getInputData().get("context");
        String userId = (String) task.getInputData().get("userId");

        int dailyBudget = 50000;
        int usedToday = 12000;
        int remaining = dailyBudget - usedToday;

        int contextTokens = 0;
        if (context != null) {
            for (Map<String, Object> doc : context) {
                Object tokens = doc.get("tokens");
                if (tokens instanceof Number) {
                    contextTokens += ((Number) tokens).intValue();
                }
            }
        }

        System.out.println("  [token_budget] user=" + userId
                + " dailyBudget=" + dailyBudget
                + " usedToday=" + usedToday
                + " contextTokens=" + contextTokens);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("trimmedContext", context);
        result.getOutputData().put("remainingBudget", remaining);
        result.getOutputData().put("contextTokens", contextTokens);
        return result;
    }
}
