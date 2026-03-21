package datawarehouseload.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Stages incoming records into a temporary staging table.
 * Input: records (list), targetTable (string)
 * Output: stagingTable (string), stagedCount (int)
 */
public class StageDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wh_stage_data";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }
        String targetTable = (String) task.getInputData().getOrDefault("targetTable", "unknown");
        String stagingTable = "stg_" + targetTable + "_" + System.currentTimeMillis();

        System.out.println("  [stage] Staged " + records.size() + " records into \"" + stagingTable + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("stagingTable", stagingTable);
        result.getOutputData().put("stagedCount", records.size());
        return result;
    }
}
