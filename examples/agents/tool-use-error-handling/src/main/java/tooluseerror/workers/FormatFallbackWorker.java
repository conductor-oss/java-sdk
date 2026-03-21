package tooluseerror.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Formats the result from a successful fallback tool invocation.
 */
public class FormatFallbackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "te_format_fallback";
    }

    @Override
    public TaskResult execute(Task task) {
        Object resultData = task.getInputData().get("result");
        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        String source = (String) task.getInputData().get("source");
        if (source == null || source.isBlank()) {
            source = "fallback";
        }

        System.out.println("  [te_format_fallback] Formatting fallback result from: " + toolName);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("formatted", "Location: San Francisco, CA (37.7899, -122.4194)");
        result.getOutputData().put("source", "fallback");
        result.getOutputData().put("toolName", toolName);
        result.getOutputData().put("reliable", true);
        result.getOutputData().put("fallbackUsed", true);
        return result;
    }
}
