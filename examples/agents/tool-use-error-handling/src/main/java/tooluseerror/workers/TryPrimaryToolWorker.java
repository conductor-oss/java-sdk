package tooluseerror.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Attempts to call the primary tool. Performs a failure by returning
 * toolStatus="failure" with a 503 service-unavailable error.
 */
public class TryPrimaryToolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "te_try_primary_tool";
    }

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

        System.out.println("  [te_try_primary_tool] Trying primary tool: " + toolName + " for query: " + query);

        Map<String, Object> error = Map.of(
                "code", 503,
                "message", "Service temporarily unavailable",
                "tool", toolName
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("toolStatus", "failure");
        result.getOutputData().put("error", error);
        result.getOutputData().put("result", null);
        result.getOutputData().put("attemptedAt", "2026-03-08T10:00:00Z");
        return result;
    }
}
