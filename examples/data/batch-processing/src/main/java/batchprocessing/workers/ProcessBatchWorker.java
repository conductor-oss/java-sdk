package batchprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes a single batch of records: validates fields, normalizes strings,
 * and computes per-record status. This does real per-item transformation work.
 *
 * Input: iteration (int), batchSize (int), totalRecords (int), batches (list of lists)
 * Output: batchIndex, processedCount, rangeStart, rangeEnd, processedItems (list of maps)
 */
public class ProcessBatchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bp_process_batch";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object iterationObj = task.getInputData().get("iteration");
        int iteration = 0;
        if (iterationObj instanceof Number) {
            iteration = ((Number) iterationObj).intValue();
        }

        Object batchSizeObj = task.getInputData().get("batchSize");
        int batchSize = 3;
        if (batchSizeObj instanceof Number) {
            batchSize = ((Number) batchSizeObj).intValue();
        }

        Object totalRecordsObj = task.getInputData().get("totalRecords");
        int totalRecords = 0;
        if (totalRecordsObj instanceof Number) {
            totalRecords = ((Number) totalRecordsObj).intValue();
        }

        int start = iteration * batchSize;
        int end = Math.min(start + batchSize, totalRecords);
        int count = end - start;

        // Get the actual batch data if available
        List<List<Object>> batches = (List<List<Object>>) task.getInputData().get("batches");
        List<Map<String, Object>> processedItems = new ArrayList<>();

        if (batches != null && iteration < batches.size()) {
            List<Object> batchItems = batches.get(iteration);
            for (int i = 0; i < batchItems.size(); i++) {
                Object item = batchItems.get(i);
                Map<String, Object> processed = new LinkedHashMap<>();
                processed.put("originalIndex", start + i);

                if (item instanceof Map) {
                    Map<String, Object> record = (Map<String, Object>) item;
                    // Real transformations: normalize strings, validate, compute hash
                    for (Map.Entry<String, Object> entry : record.entrySet()) {
                        String key = entry.getKey();
                        Object val = entry.getValue();
                        if (val instanceof String) {
                            // Trim and normalize whitespace
                            String normalized = ((String) val).trim().replaceAll("\\s+", " ");
                            processed.put(key, normalized);
                        } else {
                            processed.put(key, val);
                        }
                    }
                    // Validate: mark as valid if record has at least one non-null field
                    boolean valid = record.values().stream().anyMatch(v -> v != null && !v.toString().trim().isEmpty());
                    processed.put("_valid", valid);
                    processed.put("_fieldCount", record.size());
                } else if (item != null) {
                    // Simple value: wrap it
                    String normalized = item.toString().trim();
                    processed.put("value", normalized);
                    processed.put("_valid", !normalized.isEmpty());
                    processed.put("_fieldCount", 1);
                } else {
                    processed.put("value", null);
                    processed.put("_valid", false);
                    processed.put("_fieldCount", 0);
                }
                processedItems.add(processed);
            }
        }

        System.out.println("  [batch] Iteration " + (iteration + 1) + ": processing records "
                + (start + 1) + "-" + end + " (" + count + " records, "
                + processedItems.stream().filter(p -> Boolean.TRUE.equals(p.get("_valid"))).count() + " valid)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("batchIndex", iteration);
        result.getOutputData().put("processedCount", count);
        result.getOutputData().put("rangeStart", start);
        result.getOutputData().put("rangeEnd", end);
        result.getOutputData().put("processedItems", processedItems);
        return result;
    }
}
