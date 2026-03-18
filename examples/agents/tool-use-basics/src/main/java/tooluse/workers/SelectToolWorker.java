package tooluse.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Selects the appropriate tool based on the analyzed intent.
 * Maps intent to tool name, produces a description, and builds tool arguments.
 */
public class SelectToolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tu_select_tool";
    }

    @Override
    public TaskResult execute(Task task) {
        String intent = (String) task.getInputData().get("intent");
        if (intent == null || intent.isBlank()) {
            intent = "general";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) task.getInputData().get("analysis");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> availableTools =
                (List<Map<String, Object>>) task.getInputData().get("availableTools");

        System.out.println("  [tu_select_tool] Selecting tool for intent: " + intent);

        String selectedTool;
        String toolDescription;
        Map<String, Object> toolArgs;

        switch (intent) {
            case "get_weather":
                selectedTool = "weather_api";
                toolDescription = "Fetches current weather data for a specified location";
                toolArgs = Map.of(
                        "location", "San Francisco, CA",
                        "units", "fahrenheit"
                );
                break;
            case "calculate":
                selectedTool = "calculator";
                toolDescription = "Performs mathematical calculations";
                toolArgs = Map.of(
                        "expression", "0",
                        "precision", 2
                );
                break;
            case "search":
                selectedTool = "web_search";
                toolDescription = "Searches the web for relevant information";
                toolArgs = Map.of(
                        "query", "search query",
                        "maxResults", 5
                );
                break;
            default:
                selectedTool = "general_handler";
                toolDescription = "Handles general requests that do not match a specific tool";
                toolArgs = Map.of(
                        "request", intent
                );
                break;
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("selectedTool", selectedTool);
        result.getOutputData().put("toolDescription", toolDescription);
        result.getOutputData().put("toolArgs", toolArgs);
        return result;
    }
}
