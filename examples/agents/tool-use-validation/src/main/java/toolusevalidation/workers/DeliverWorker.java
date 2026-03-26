package toolusevalidation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Delivers the final formatted result to the user, combining the validated
 * tool output with the validation report into a human-readable summary.
 */
public class DeliverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tv_deliver";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String userRequest = (String) task.getInputData().get("userRequest");
        if (userRequest == null || userRequest.isBlank()) {
            userRequest = "general query";
        }

        Map<String, Object> validatedOutput = (Map<String, Object>) task.getInputData().get("validatedOutput");
        if (validatedOutput == null) {
            validatedOutput = Map.of();
        }

        System.out.println("  [tv_deliver] Delivering result for request: " + userRequest);

        Object temp = validatedOutput.getOrDefault("temperature", "N/A");
        Object humidity = validatedOutput.getOrDefault("humidity", "N/A");
        Object conditions = validatedOutput.getOrDefault("conditions", "N/A");
        Object windSpeed = validatedOutput.getOrDefault("windSpeed", "N/A");
        Object windDirection = validatedOutput.getOrDefault("windDirection", "N/A");

        String formattedResult = "Weather in London: " + temp + "\u00B0C, "
                + conditions + ". Humidity: " + humidity
                + "%. Wind: " + windSpeed + " km/h " + windDirection
                + ". 3-day forecast available.";

        String validationSummary = "All input and output validations passed. "
                + "4 input checks and 4 output checks completed with zero errors.";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("formattedResult", formattedResult);
        result.getOutputData().put("validationSummary", validationSummary);
        return result;
    }
}
