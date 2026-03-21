package datavalidation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Loads records for validation.
 * Input: records (list of maps)
 * Output: records, count
 */
public class LoadRecordsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vd_load_records";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        System.out.println("  [vd_load_records] Loaded " + records.size() + " records for validation");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("count", records.size());
        return result;
    }
}
