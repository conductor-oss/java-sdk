package eventschemavalidation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Validates that an incoming event contains all required fields defined by the
 * schema.  Required fields are: "type", "source", and "data".
 *
 * Input:  { event: Map, schemaName: String }
 * Output: { result: "valid"|"invalid", errors: List<String>, schemaUsed: String }
 */
public class ValidateSchemaWorker implements Worker {

    private static final List<String> REQUIRED_FIELDS = Arrays.asList("type", "source", "data");

    @Override
    public String getTaskDefName() {
        return "sv_validate_schema";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> event = (Map<String, Object>) task.getInputData().get("event");
        String schemaName = (String) task.getInputData().get("schemaName");

        if (schemaName == null || schemaName.isBlank()) {
            schemaName = "default_schema";
        }

        System.out.println("  [sv_validate_schema] Validating event against schema: " + schemaName);

        List<String> errors = new ArrayList<>();

        if (event == null) {
            for (String field : REQUIRED_FIELDS) {
                errors.add("Missing required field: " + field);
            }
        } else {
            for (String field : REQUIRED_FIELDS) {
                if (!event.containsKey(field)) {
                    errors.add("Missing required field: " + field);
                }
            }
        }

        String result = errors.isEmpty() ? "valid" : "invalid";

        TaskResult taskResult = new TaskResult(task);
        taskResult.setStatus(TaskResult.Status.COMPLETED);
        taskResult.getOutputData().put("result", result);
        taskResult.getOutputData().put("errors", errors);
        taskResult.getOutputData().put("schemaUsed", schemaName);
        return taskResult;
    }
}
