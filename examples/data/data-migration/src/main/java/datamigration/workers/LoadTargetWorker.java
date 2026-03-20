package datamigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Loads transformed records into the target system.
 * Input: transformedRecords, targetConfig
 * Output: loadedCount, failedCount, targetTable
 */
public class LoadTargetWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mi_load_target";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("transformedRecords");
        if (records == null) {
            records = List.of();
        }

        System.out.println("  [load] Loaded " + records.size() + " records into target system (batch insert)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("loadedCount", records.size());
        result.getOutputData().put("failedCount", 0);
        result.getOutputData().put("targetTable", "employees_v2");
        return result;
    }
}
