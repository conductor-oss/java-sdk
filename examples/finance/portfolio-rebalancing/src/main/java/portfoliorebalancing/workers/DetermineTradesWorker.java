package portfoliorebalancing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DetermineTradesWorker implements Worker {
    @Override public String getTaskDefName() { return "prt_determine_trades"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> analysis = (List<Map<String, Object>>) task.getInputData().get("driftAnalysis");
        if (analysis == null) analysis = List.of();
        List<Map<String, Object>> trades = new ArrayList<>();
        for (Map<String, Object> a : analysis) {
            double drift = a.get("drift") instanceof Number ? ((Number) a.get("drift")).doubleValue() : 0;
            if (Math.abs(drift) > 1) {
                trades.add(Map.of("asset", a.get("asset"), "action", drift > 0 ? "SELL" : "BUY", "percentChange", Math.abs(drift)));
            }
        }
        System.out.println("  [trades] " + trades.size() + " rebalancing trades needed");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("trades", trades);
        result.getOutputData().put("taxLotMethod", "specific_id");
        return result;
    }
}
