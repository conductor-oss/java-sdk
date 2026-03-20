package datamasking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Loads records for PII masking.
 * Input: records (list)
 * Output: records (pass-through), count
 */
public class LoadRecordsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mk_load_records";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("records", List.of());

        System.out.println("  [load] Loaded " + records.size() + " records for PII masking");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("count", records.size());
        return result;
    }
}
