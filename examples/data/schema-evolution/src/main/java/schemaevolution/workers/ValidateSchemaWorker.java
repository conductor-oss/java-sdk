package schemaevolution.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Validates transformed data against the target schema.
 * Input: transformedData, targetSchema
 * Output: passed, compatibilityLevel, validatedRecords
 */
public class ValidateSchemaWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sh_validate_schema";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) task.getInputData().get("transformedData");
        if (data == null) {
            data = List.of();
        }

        boolean allValid = data.stream().allMatch(r ->
                r.containsKey("phone_number")
                && r.get("age") instanceof Integer
                && !r.containsKey("legacy_id"));

        String compatibilityLevel = allValid ? "FULL" : "PARTIAL";

        System.out.println("  [validate] Validation " + (allValid ? "PASSED" : "FAILED") + " — compatibility: " + compatibilityLevel);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("passed", allValid);
        result.getOutputData().put("compatibilityLevel", compatibilityLevel);
        result.getOutputData().put("validatedRecords", data.size());
        return result;
    }
}
