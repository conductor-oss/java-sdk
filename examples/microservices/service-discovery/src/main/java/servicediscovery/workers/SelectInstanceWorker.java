package servicediscovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Selects the best service instance based on strategy (least-connections).
 * Input: instances, strategy
 * Output: selectedInstance, strategy
 */
public class SelectInstanceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sd_select_instance";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> instances = (List<Map<String, Object>>) task.getInputData().get("instances");
        String strategy = (String) task.getInputData().get("strategy");
        if (strategy == null) strategy = "least-connections";
        if (instances == null) instances = List.of();

        // Filter healthy instances and pick the one with fewest connections
        Map<String, Object> selected = instances.stream()
                .filter(i -> "healthy".equals(i.get("health")))
                .min(Comparator.comparingInt(i -> ((Number) i.get("connections")).intValue()))
                .orElse(instances.isEmpty() ? Map.of("id", "none", "host", "0.0.0.0", "port", 0, "health", "unknown", "connections", 0) : instances.get(0));

        System.out.println("  [sd_select_instance] Selected instance " + selected.get("id") + " (" + selected.get("connections") + " connections)...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("selectedInstance", selected);
        result.getOutputData().put("strategy", strategy);
        return result;
    }
}
