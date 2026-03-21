package loadbalancing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Aggregates results from all parallel instance calls.
 * Input: result1, result2, result3
 * Output: aggregated, totalProcessed, avgLatency
 */
public class AggregateResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lb_aggregate_results";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> r1 = (Map<String, Object>) task.getInputData().get("result1");
        Map<String, Object> r2 = (Map<String, Object>) task.getInputData().get("result2");
        Map<String, Object> r3 = (Map<String, Object>) task.getInputData().get("result3");

        List<Map<String, Object>> results = List.of(
                r1 != null ? r1 : Map.of(),
                r2 != null ? r2 : Map.of(),
                r3 != null ? r3 : Map.of()
        );

        int total = results.stream()
                .mapToInt(r -> r.get("recordsProcessed") instanceof Number ? ((Number) r.get("recordsProcessed")).intValue() : 0)
                .sum();

        int avgLatency = (int) Math.round(results.stream()
                .mapToInt(r -> r.get("latencyMs") instanceof Number ? ((Number) r.get("latencyMs")).intValue() : 0)
                .average().orElse(0));

        System.out.println("  [aggregate] Combined " + total + " records from " + results.size() + " instances...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregated", results);
        result.getOutputData().put("totalProcessed", total);
        result.getOutputData().put("avgLatency", avgLatency);
        return result;
    }
}
