package portfoliorebalancing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecuteTradesWorker implements Worker {
    @Override public String getTaskDefName() { return "prt_execute_trades"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> trades = (List<Map<String, Object>>) task.getInputData().get("trades");
        if (trades == null) trades = List.of();
        System.out.println("  [execute] Executing " + trades.size() + " trades for account " + task.getInputData().get("accountId"));
        List<Map<String, Object>> executed = new ArrayList<>();
        for (int i = 0; i < trades.size(); i++) {
            Map<String, Object> t = new HashMap<>(trades.get(i));
            t.put("executionId", "EXEC-RB-" + (i + 1));
            t.put("filled", true);
            executed.add(t);
        }
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("executedTrades", executed);
        result.getOutputData().put("tradeCount", executed.size());
        return result;
    }
}
