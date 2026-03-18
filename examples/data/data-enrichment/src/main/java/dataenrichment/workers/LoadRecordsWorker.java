package dataenrichment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Loads incoming records for enrichment.
 * Input: records (list of record maps)
 * Output: records (pass-through), count (number of records)
 */
public class LoadRecordsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dr_load_records";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        System.out.println("  [load] Loaded " + records.size() + " records for enrichment");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("count", records.size());
        return result;
    }
}
