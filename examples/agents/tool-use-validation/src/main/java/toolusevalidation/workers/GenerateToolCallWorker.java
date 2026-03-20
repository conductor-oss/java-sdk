package toolusevalidation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Generates a tool call from a user request, producing structured
 * tool arguments along with input and output schemas for validation.
 */
public class GenerateToolCallWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tv_generate_tool_call";
    }

    @Override
    public TaskResult execute(Task task) {
        String userRequest = (String) task.getInputData().get("userRequest");
        if (userRequest == null || userRequest.isBlank()) {
            userRequest = "general query";
        }

        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "generic_tool";
        }

        System.out.println("  [tv_generate_tool_call] Generating tool call for: " + toolName
                + " (request: " + userRequest + ")");

        Map<String, Object> toolArgs = Map.of(
                "city", "London",
                "country", "UK",
                "units", "metric",
                "include", List.of("temperature", "humidity", "wind", "forecast")
        );

        Map<String, Object> inputSchema = Map.of(
                "type", "object",
                "required", List.of("city", "country"),
                "properties", Map.of(
                        "city", Map.of("type", "string"),
                        "country", Map.of("type", "string"),
                        "units", Map.of("type", "string", "enum", List.of("metric", "imperial")),
                        "include", Map.of("type", "array", "items", Map.of("type", "string"))
                )
        );

        Map<String, Object> outputSchema = Map.of(
                "type", "object",
                "required", List.of("temperature", "humidity", "conditions"),
                "properties", Map.of(
                        "temperature", Map.of("type", "number"),
                        "humidity", Map.of("type", "number"),
                        "conditions", Map.of("type", "string"),
                        "windSpeed", Map.of("type", "number")
                )
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("toolArgs", toolArgs);
        result.getOutputData().put("inputSchema", inputSchema);
        result.getOutputData().put("outputSchema", outputSchema);
        return result;
    }
}
