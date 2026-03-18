package dataenrichment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Merges all enriched data and produces a summary by counting
 * how many enrichment fields were actually added to records.
 * Input: records (fully enriched list), originalCount
 * Output: enrichedCount, fieldsAdded (actual count), summary, records
 */
public class MergeEnrichedWorker implements Worker {

    // Fields that are added by enrichment workers
    private static final Set<String> ENRICHMENT_FIELDS = Set.of("geo", "company", "credit", "dns");

    @Override
    public String getTaskDefName() {
        return "dr_merge_enriched";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        Object originalCountObj = task.getInputData().get("originalCount");
        int originalCount = 0;
        if (originalCountObj instanceof Number) {
            originalCount = ((Number) originalCountObj).intValue();
        }

        // Count distinct enrichment fields that were actually added across all records
        Set<String> addedFields = new LinkedHashSet<>();
        for (Map<String, Object> record : records) {
            for (String field : ENRICHMENT_FIELDS) {
                if (record.containsKey(field)) {
                    addedFields.add(field);
                }
            }
        }

        int fieldsAdded = addedFields.size();
        String summary = "Enriched " + records.size() + "/" + originalCount
                + " records with " + fieldsAdded + " data sources";

        System.out.println("  [merge] " + summary + " (" + String.join(", ", addedFields) + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enrichedCount", records.size());
        result.getOutputData().put("fieldsAdded", fieldsAdded);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("records", records);
        return result;
    }
}
