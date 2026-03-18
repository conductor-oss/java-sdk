package fanoutfanin.workers;

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
 * collects the output from each img_N_ref entry, and calculates:
 * - totalOriginal: sum of all original sizes
 * - totalProcessed: sum of all processed sizes
 * - savings: percentage reduction = (1 - totalProcessed/totalOriginal) * 100
 */
public class AggregateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fo_aggregate";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> joinOutput = (Map<String, Object>) task.getInputData().get("joinOutput");

        List<Map<String, Object>> results = new ArrayList<>();
        int totalOriginal = 0;
        int totalProcessed = 0;

        if (joinOutput != null) {
            // Sort by key to get deterministic ordering (img_0_ref, img_1_ref, ...)
            TreeMap<String, Object> sorted = new TreeMap<>(joinOutput);
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("img_") && key.endsWith("_ref")) {
                    Map<String, Object> taskOutput = (Map<String, Object>) entry.getValue();
                    if (taskOutput != null) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", taskOutput.get("name"));
                        item.put("originalSize", taskOutput.get("originalSize"));
                        item.put("processedSize", taskOutput.get("processedSize"));
                        item.put("format", taskOutput.get("format"));
                        item.put("processingTime", taskOutput.get("processingTime"));
                        results.add(item);

                        Object origObj = taskOutput.get("originalSize");
                        if (origObj instanceof Number) {
                            totalOriginal += ((Number) origObj).intValue();
                        }
                        Object procObj = taskOutput.get("processedSize");
                        if (procObj instanceof Number) {
                            totalProcessed += ((Number) procObj).intValue();
                        }
                    }
                }
            }
        }

        // Calculate savings percentage
        double savings = 0.0;
        if (totalOriginal > 0) {
            savings = (1.0 - (double) totalProcessed / totalOriginal) * 100.0;
        }

        System.out.println("  [fo_aggregate] Aggregated " + results.size()
                + " results, savings=" + String.format("%.1f", savings) + "%");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processedCount", results.size());
        result.getOutputData().put("totalOriginal", totalOriginal);
        result.getOutputData().put("totalProcessed", totalProcessed);
        result.getOutputData().put("savings", Math.round(savings * 100.0) / 100.0);
        result.getOutputData().put("results", results);
        return result;
    }
}
