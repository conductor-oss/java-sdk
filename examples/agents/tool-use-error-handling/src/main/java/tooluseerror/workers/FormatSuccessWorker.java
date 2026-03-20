package tooluseerror.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Formats the result from a successful primary tool invocation.
 */
public class FormatSuccessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "te_format_success";
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
            source = "primary";
        }

        System.out.println("  [te_format_success] Formatting success result from: " + toolName);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("formatted", resultData);
        result.getOutputData().put("source", "primary");
        result.getOutputData().put("toolName", toolName);
        result.getOutputData().put("reliable", true);
        return result;
    }
}
