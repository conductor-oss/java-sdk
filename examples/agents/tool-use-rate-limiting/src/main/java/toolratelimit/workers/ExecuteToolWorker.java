package toolratelimit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Executes the tool immediately when the rate limit allows it.
 * Returns a default translation result with executedImmediately=true.
 */
public class ExecuteToolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rl_execute_tool";
    }

    @Override
    public TaskResult execute(Task task) {
        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        System.out.println("  [rl_execute_tool] Executing tool immediately: " + toolName);

        Map<String, Object> translationResult = Map.of(
                "translation", "Bonjour, comment allez-vous aujourd'hui?",
                "sourceLanguage", "en",
                "targetLanguage", "fr",
                "confidence", 0.98
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", translationResult);
        result.getOutputData().put("executedImmediately", true);
        result.getOutputData().put("executionTimeMs", 180);
        return result;
    }
}
