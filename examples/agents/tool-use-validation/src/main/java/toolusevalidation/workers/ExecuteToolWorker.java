package toolusevalidation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Executes the validated tool call and returns the raw output
 * along with execution metadata.
 */
public class ExecuteToolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tv_execute_tool";
    }

    @Override
    public TaskResult execute(Task task) {
        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        System.out.println("  [tv_execute_tool] Executing tool: " + toolName);

        Map<String, Object> rawOutput = Map.of(
                "temperature", 14.2,
                "humidity", 72,
                "conditions", "partly cloudy",
                "windSpeed", 18.5,
                "windDirection", "SW",
                "forecast", List.of(
                        Map.of("day", "Monday", "high", 16, "low", 11, "conditions", "cloudy"),
                        Map.of("day", "Tuesday", "high", 18, "low", 12, "conditions", "sunny"),
                        Map.of("day", "Wednesday", "high", 15, "low", 10, "conditions", "rain")
                ),
                "provider", "weather_api_v3",
                "timestamp", "2026-03-08T10:00:00Z"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rawOutput", rawOutput);
        result.getOutputData().put("executionTimeMs", 245);
        return result;
    }
}
