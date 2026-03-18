package dynamicfork.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Aggregates results from all dynamic fork branches.
 *
 * Takes the joinOutput map (keyed by taskReferenceName) produced by the JOIN task,
 * collects the output from each fetch_N_ref entry, and returns a summary.
 */
public class AggregateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "df_aggregate";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> joinOutput = (Map<String, Object>) task.getInputData().get("joinOutput");

        List<Map<String, Object>> results = new ArrayList<>();
        int totalSize = 0;

        if (joinOutput != null) {
            // Sort by key to get deterministic ordering (fetch_0_ref, fetch_1_ref, ...)
            TreeMap<String, Object> sorted = new TreeMap<>(joinOutput);
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("fetch_") && key.endsWith("_ref")) {
                    Map<String, Object> taskOutput = (Map<String, Object>) entry.getValue();
                    if (taskOutput != null) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("url", taskOutput.get("url"));
                        item.put("status", taskOutput.get("status"));
                        item.put("size", taskOutput.get("size"));
                        item.put("loadTime", taskOutput.get("loadTime"));
                        results.add(item);

                        Object sizeObj = taskOutput.get("size");
                        if (sizeObj instanceof Number) {
                            totalSize += ((Number) sizeObj).intValue();
                        }
                    }
                }
            }
        }

        System.out.println("  [df_aggregate] Aggregated " + results.size() + " results, totalSize=" + totalSize);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        result.getOutputData().put("totalProcessed", results.size());
        result.getOutputData().put("totalSize", totalSize);
        return result;
    }
}
