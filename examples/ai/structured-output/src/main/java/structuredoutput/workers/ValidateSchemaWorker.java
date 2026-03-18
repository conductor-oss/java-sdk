package structuredoutput.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Worker that validates JSON data against a schema.
 * Checks that all required fields exist and that their types match the schema.
 */
public class ValidateSchemaWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "so_validate_schema";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("jsonOutput");
        Map<String, Object> schema = (Map<String, Object>) task.getInputData().get("schema");

        System.out.println("  [so_validate_schema] Validating data against schema...");

        List<String> requiredFields = (List<String>) schema.get("required");
        Map<String, String> types = (Map<String, String>) schema.get("types");

        List<String> errors = new ArrayList<>();

        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                errors.add("Missing required field: " + field);
            } else {
                String expectedType = types.get(field);
                if (expectedType != null) {
                    Object value = data.get(field);
                    boolean typeMatch = switch (expectedType) {
                        case "string" -> value instanceof String;
                        case "number" -> value instanceof Number;
                        default -> true;
                    };
                    if (!typeMatch) {
                        errors.add("Type mismatch for field '" + field
                                + "': expected " + expectedType
                                + ", got " + (value == null ? "null" : value.getClass().getSimpleName()));
                    }
                }
            }
        }

        boolean valid = errors.isEmpty();

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", valid);
        result.getOutputData().put("errors", errors);
        result.getOutputData().put("data", data);
        result.getOutputData().put("validated", true);
        result.getOutputData().put("fieldCount", data.size());
        return result;
    }
}
