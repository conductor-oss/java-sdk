package datadedup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes dedup keys from specified match fields.
 * Each record gets a dedupKey built from lowercased/trimmed values of matchFields joined by "|".
 * Input: records (list), matchFields (list of field names)
 * Output: keyedRecords (list with dedupKey added)
 */
public class ComputeKeysWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dp_compute_keys";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        List<String> matchFields = (List<String>) task.getInputData().get("matchFields");
        if (matchFields == null) {
            matchFields = List.of("email");
        }

        List<String> fields = matchFields;
        List<Map<String, Object>> keyedRecords = records.stream().map(r -> {
            Map<String, Object> keyed = new LinkedHashMap<>(r);
            String dedupKey = fields.stream()
                    .map(f -> {
                        Object val = r.get(f);
                        return val != null ? val.toString().toLowerCase().trim() : "";
                    })
                    .collect(Collectors.joining("|"));
            keyed.put("dedupKey", dedupKey);
            return keyed;
        }).collect(Collectors.toList());

        System.out.println("  [dp_compute_keys] Computed dedup keys on fields: " + matchFields);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("keyedRecords", keyedRecords);
        return result;
    }
}
