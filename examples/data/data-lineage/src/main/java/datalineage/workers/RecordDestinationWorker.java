package datalineage.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Records the destination in the lineage chain.
 * Input: records (list), lineage (list), destName (string)
 * Output: records (pass-through), lineage (list with destination entry)
 */
public class RecordDestinationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ln_record_destination";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }
        List<Map<String, Object>> prevLineage = (List<Map<String, Object>>) task.getInputData().get("lineage");
        if (prevLineage == null) {
            prevLineage = List.of();
        }
        String destName = (String) task.getInputData().getOrDefault("destName", "unknown");

        List<Map<String, Object>> lineage = new ArrayList<>(prevLineage);
        lineage.add(Map.of(
                "step", "destination",
                "name", destName,
                "timestamp", Instant.now().toString(),
                "recordCount", records.size(),
                "operation", "load"
        ));

        System.out.println("  [destination] Recorded destination \"" + destName + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("lineage", lineage);
        return result;
    }
}
