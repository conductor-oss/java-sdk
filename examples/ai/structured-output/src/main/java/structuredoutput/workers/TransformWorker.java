package structuredoutput.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Worker that transforms validated data by adding enrichment fields.
 * Adds _validated and _timestamp to the data map.
 */
public class TransformWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "so_transform";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Boolean validated = (Boolean) task.getInputData().get("validated");
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("data");

        System.out.println("  [so_transform] Transforming data, validated=" + validated);

        Map<String, Object> enrichedData = new LinkedHashMap<>(data);
        enrichedData.put("_validated", true);
        enrichedData.put("_timestamp", "2026-03-07T00:00:00Z");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("final", enrichedData);
        return result;
    }
}
