package healthcheckaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class AggregateHealthWorker implements Worker {
    @Override public String getTaskDefName() { return "hc_aggregate_health"; }
    @Override @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        var components = List.of(
            (Map<String, Object>) task.getInputData().getOrDefault("api", Map.of()),
            (Map<String, Object>) task.getInputData().getOrDefault("db", Map.of()),
            (Map<String, Object>) task.getInputData().getOrDefault("cache", Map.of()),
            (Map<String, Object>) task.getInputData().getOrDefault("queue", Map.of()));
        boolean allHealthy = components.stream().allMatch(c -> Boolean.TRUE.equals(c.get("healthy")));
        System.out.println("  [aggregate] Overall: " + (allHealthy ? "HEALTHY" : "DEGRADED"));
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("status", allHealthy ? "healthy" : "degraded");
        r.getOutputData().put("details", components);
        return r;
    }
}
