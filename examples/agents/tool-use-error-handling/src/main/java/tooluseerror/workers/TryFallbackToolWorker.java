package tooluseerror.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Attempts the fallback tool after the primary tool has failed.
 * Returns a successful geocoding result.
 */
public class TryFallbackToolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "te_try_fallback_tool";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        if (query == null || query.isBlank()) {
            query = "";
        }

        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        Map<String, Object> primaryError = (Map<String, Object>) task.getInputData().get("primaryError");
        String fallbackReason = "unknown error";
        if (primaryError != null && primaryError.get("message") != null) {
            fallbackReason = (String) primaryError.get("message");
        }

        System.out.println("  [te_try_fallback_tool] Trying fallback tool: " + toolName + " for query: " + query);

        Map<String, Object> geocodingResult = Map.of(
                "location", "San Francisco, CA",
                "coordinates", Map.of(
                        "lat", 37.7899,
                        "lng", -122.4194
                ),
                "confidence", 0.97,
                "provider", toolName
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("toolStatus", "success");
        result.getOutputData().put("result", geocodingResult);
        result.getOutputData().put("fallbackReason", fallbackReason);
        return result;
    }
}
