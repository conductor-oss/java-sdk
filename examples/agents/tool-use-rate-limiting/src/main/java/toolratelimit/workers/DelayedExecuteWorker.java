package toolratelimit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Executes a previously queued tool request after a delay.
 * Returns the same translation result with executedImmediately=false.
 */
public class DelayedExecuteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rl_delayed_execute";
    }

    @Override
    public TaskResult execute(Task task) {
        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        String queueId = (String) task.getInputData().get("queueId");
        if (queueId == null || queueId.isBlank()) {
            queueId = "q-unknown";
        }

        System.out.println("  [rl_delayed_execute] Executing delayed request: " + toolName
                + " (queueId: " + queueId + ")");

        Map<String, Object> translationResult = Map.of(
                "translation", "Bonjour, comment allez-vous aujourd'hui?",
                "sourceLanguage", "en",
                "targetLanguage", "fr",
                "confidence", 0.98
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", translationResult);
        result.getOutputData().put("executedImmediately", false);
        result.getOutputData().put("delayedByMs", 5000);
        result.getOutputData().put("queueId", queueId);
        result.getOutputData().put("executionTimeMs", 195);
        return result;
    }
}
