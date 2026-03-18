package tooluse.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Formats tool execution results into a natural language answer.
 * Builds a human-readable string based on the tool output.
 */
public class FormatResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tu_format_result";
    }

    @Override
    public TaskResult execute(Task task) {
        String userRequest = (String) task.getInputData().get("userRequest");
        if (userRequest == null || userRequest.isBlank()) {
            userRequest = "your request";
        }

        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> toolOutput = (Map<String, Object>) task.getInputData().get("toolOutput");

        System.out.println("  [tu_format_result] Formatting result from tool: " + toolName);

        String answer;

        if ("weather_api".equals(toolName) && toolOutput != null) {
            String location = String.valueOf(toolOutput.getOrDefault("location", "the requested location"));
            Object temperature = toolOutput.getOrDefault("temperature", "N/A");
            String units = String.valueOf(toolOutput.getOrDefault("units", "fahrenheit"));
            String condition = String.valueOf(toolOutput.getOrDefault("condition", "Unknown"));
            Object humidity = toolOutput.getOrDefault("humidity", "N/A");
            Object windSpeed = toolOutput.getOrDefault("windSpeed", "N/A");
            String windDirection = String.valueOf(toolOutput.getOrDefault("windDirection", "N/A"));

            String unitSymbol = "fahrenheit".equals(units) ? "F" : "C";

            answer = "The current weather in " + location + " is " + temperature + " degrees " + unitSymbol
                    + " and " + condition + ". Humidity is at " + humidity + "% with winds from the "
                    + windDirection + " at " + windSpeed + " mph.";
        } else if ("calculator".equals(toolName) && toolOutput != null) {
            answer = "The result of the calculation is: " + toolOutput.getOrDefault("result", "N/A") + ".";
        } else if ("web_search".equals(toolName) && toolOutput != null) {
            answer = "Here are the search results for your query. Found "
                    + toolOutput.getOrDefault("totalResults", 0) + " results.";
        } else {
            answer = "I processed " + userRequest + " using " + toolName
                    + " but could not generate a detailed response.";
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("toolUsed", toolName);
        result.getOutputData().put("sourceData", toolOutput);
        return result;
    }
}
