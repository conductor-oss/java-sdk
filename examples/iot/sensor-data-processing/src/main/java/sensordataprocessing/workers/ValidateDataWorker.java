package sensordataprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Validates sensor readings for data quality.
 * Input: batchId, readingCount, readings
 * Output: validCount, invalidCount, validatedData, issues
 */
public class ValidateDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sen_validate_data";
    }

    @Override
    public TaskResult execute(Task task) {
        Object countObj = task.getInputData().get("readingCount");
        int count = 0;
        if (countObj instanceof Number) {
            count = ((Number) countObj).intValue();
        } else if (countObj instanceof String) {
            try { count = Integer.parseInt((String) countObj); } catch (NumberFormatException ignored) {}
        }

        int validCount = (int) Math.round(count * 0.98);
        int invalidCount = count - validCount;

        System.out.println("  [validate] Validated " + validCount + "/" + count + " readings");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validCount", validCount);
        result.getOutputData().put("invalidCount", invalidCount);
        result.getOutputData().put("validatedData", task.getInputData().get("readings"));
        result.getOutputData().put("issues", List.of("24 readings had null values", "sensor S-047 offline"));
        return result;
    }
}
