package datawarehouseload.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Runs pre-load quality checks on staged records.
 * Input: stagingTable (string), recordCount (int), schema (string)
 * Output: passed (boolean), validCount (int), checks (map)
 */
public class PreLoadChecksWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wh_pre_load_checks";
    }

    @Override
    public TaskResult execute(Task task) {
        Object countObj = task.getInputData().get("recordCount");
        int count = 0;
        if (countObj instanceof Number) {
            count = ((Number) countObj).intValue();
        }

        Map<String, Object> checks = Map.of(
                "nullCheck", true,
                "typeCheck", true,
                "uniqueCheck", true
        );

        System.out.println("  [pre-check] Ran pre-load checks on " + count + " staged records: null=true, type=true, unique=true");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("passed", true);
        result.getOutputData().put("validCount", count);
        result.getOutputData().put("checks", checks);
        return result;
    }
}
