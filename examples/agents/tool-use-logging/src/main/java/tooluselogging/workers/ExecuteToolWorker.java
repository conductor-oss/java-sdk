package tooluselogging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Executes a tool (sentiment analysis).
 * Takes toolName, toolArgs, requestId and returns a fixed sentiment analysis result.
 */
public class ExecuteToolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tl_execute_tool";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        Map<String, Object> toolArgs = null;
        Object toolArgsObj = task.getInputData().get("toolArgs");
        if (toolArgsObj instanceof Map) {
            toolArgs = (Map<String, Object>) toolArgsObj;
        }

        String requestId = (String) task.getInputData().get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req-unknown";
        }

        String text = "";
        if (toolArgs != null && toolArgs.get("text") instanceof String) {
            text = (String) toolArgs.get("text");
        }

        System.out.println("  [tl_execute_tool] Executing tool: " + toolName + " (requestId=" + requestId + ")");

        Map<String, Object> emotions = Map.of(
                "joy", 0.72,
                "trust", 0.65,
                "anticipation", 0.58,
                "surprise", 0.12,
                "anger", 0.02,
                "sadness", 0.05
        );

        Map<String, Object> sentimentResult = Map.of(
                "text", text,
                "sentiment", "positive",
                "confidence", 0.94,
                "emotions", emotions,
                "language", "en",
                "wordCount", 15
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", sentimentResult);
        result.getOutputData().put("executionTimeMs", 187);
        result.getOutputData().put("toolStatus", "success");
        return result;
    }
}
