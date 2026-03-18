package tooluseparallel.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Plan tools worker — analyzes the user request and determines which tools to invoke
 * in parallel. Returns tool configurations for weather, news, and stocks APIs.
 */
public class PlanToolsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tp_plan_tools";
    }

    @Override
    public TaskResult execute(Task task) {
        String userRequest = (String) task.getInputData().get("userRequest");
        if (userRequest == null || userRequest.isBlank()) {
            userRequest = "morning briefing";
        }

        String location = (String) task.getInputData().get("location");
        if (location == null || location.isBlank()) {
            location = "New York, NY";
        }

        System.out.println("  [tp_plan_tools] Planning tools for request: " + userRequest + " (location: " + location + ")");

        Map<String, Object> weatherConfig = Map.of(
                "units", "fahrenheit",
                "includeHourly", true
        );

        List<String> newsTopics = List.of("technology", "business", "world");

        List<String> tickers = List.of("AAPL", "GOOGL", "MSFT", "AMZN");

        Map<String, Object> toolConfigs = Map.of(
                "weather", weatherConfig,
                "newsTopics", newsTopics,
                "tickers", tickers
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("toolConfigs", toolConfigs);
        result.getOutputData().put("toolCount", 3);
        return result;
    }
}
