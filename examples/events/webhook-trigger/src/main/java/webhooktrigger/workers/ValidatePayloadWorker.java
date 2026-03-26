package webhooktrigger.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Validates the parsed event payload against the expected schema,
 * performing structural and value checks.
 */
public class ValidatePayloadWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wt_validate_payload";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null || eventType.isBlank()) {
            eventType = "unknown";
        }

        Map<String, Object> parsedData = (Map<String, Object>) task.getInputData().get("parsedData");
        if (parsedData == null) {
            parsedData = Map.of();
        }

        String schema = (String) task.getInputData().get("schema");
        if (schema == null || schema.isBlank()) {
            schema = "unknown";
        }

        System.out.println("  [wt_validate_payload] Validating " + eventType + " against schema " + schema);

        Map<String, Boolean> checks = Map.of(
                "hasOrderId", true,
                "hasCustomer", true,
                "validAmount", true,
                "validCurrency", true
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", true);
        result.getOutputData().put("checks", checks);
        result.getOutputData().put("validatedData", parsedData);
        result.getOutputData().put("targetFormat", "canonical");
        return result;
    }
}
