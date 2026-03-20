package datalineage.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registers the data source and initializes lineage tracking.
 * Input: records (list), sourceName (string)
 * Output: records (list), lineage (list), recordCount (int)
 */
public class RegisterSourceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ln_register_source";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }
        String sourceName = (String) task.getInputData().getOrDefault("sourceName", "unknown");

        List<Map<String, Object>> lineage = new ArrayList<>();
        lineage.add(Map.of(
                "step", "source",
                "name", sourceName,
                "timestamp", Instant.now().toString(),
                "recordCount", records.size(),
                "operation", "extract"
        ));

        System.out.println("  [source] Registered source \"" + sourceName + "\" with " + records.size() + " records");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("lineage", lineage);
        result.getOutputData().put("recordCount", records.size());
        return result;
    }
}
