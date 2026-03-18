package tooluse.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Analyzes a user request to determine intent, entities, and complexity.
 * Returns an analysis object with entities, intent, complexity, and requiresMultipleTools,
 * along with a confidence score.
 */
public class AnalyzeRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tu_analyze_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String userRequest = (String) task.getInputData().get("userRequest");
        if (userRequest == null || userRequest.isBlank()) {
            userRequest = "unknown request";
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> availableTools =
                (List<Map<String, Object>>) task.getInputData().get("availableTools");

        System.out.println("  [tu_analyze_request] Analyzing request: " + userRequest);

        // Determine intent from the request text
        String intent;
        List<String> entities;
        if (userRequest.toLowerCase().contains("weather")) {
            intent = "get_weather";
            entities = List.of("weather", "location");
        } else if (userRequest.toLowerCase().contains("calculat") || userRequest.toLowerCase().contains("math")) {
            intent = "calculate";
            entities = List.of("calculation", "numbers");
        } else if (userRequest.toLowerCase().contains("search") || userRequest.toLowerCase().contains("find")) {
            intent = "search";
            entities = List.of("search", "query");
        } else {
            intent = "general";
            entities = List.of("general");
        }

        Map<String, Object> analysis = Map.of(
                "entities", entities,
                "intent", intent,
                "complexity", "simple",
                "requiresMultipleTools", false
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("analysis", analysis);
        result.getOutputData().put("intent", intent);
        result.getOutputData().put("confidence", 0.95);
        return result;
    }
}
