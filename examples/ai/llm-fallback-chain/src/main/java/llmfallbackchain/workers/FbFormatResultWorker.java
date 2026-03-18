package llmfallbackchain.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Formats the final result from the fallback chain. Inspects the status of each
 * model's response (gpt4, claude, gemini) and picks the first one that succeeded.
 * Counts how many fallbacks were triggered before success.
 */
public class FbFormatResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fb_format_result";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> input = task.getInputData();

        String gpt4Status = (String) input.get("gpt4Status");
        String gpt4Response = (String) input.get("gpt4Response");

        String claudeStatus = (String) input.get("claudeStatus");
        String claudeResponse = (String) input.get("claudeResponse");

        String geminiStatus = (String) input.get("geminiStatus");
        String geminiResponse = (String) input.get("geminiResponse");

        // Models in order: gpt4, claude, gemini
        String[] statuses = { gpt4Status, claudeStatus, geminiStatus };
        String[] responses = { gpt4Response, claudeResponse, geminiResponse };
        String[] modelNames = { "gpt-4", "claude", "gemini-pro" };

        String selectedResponse = null;
        String modelUsed = null;
        int fallbacksTriggered = 0;

        for (int i = 0; i < statuses.length; i++) {
            if ("success".equals(statuses[i])) {
                selectedResponse = responses[i];
                modelUsed = modelNames[i];
                fallbacksTriggered = i;
                break;
            }
        }

        System.out.println("  [fb_format_result] Model used: " + modelUsed
                + ", fallbacks triggered: " + fallbacksTriggered);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", selectedResponse);
        result.getOutputData().put("modelUsed", modelUsed);
        result.getOutputData().put("fallbacksTriggered", fallbacksTriggered);
        return result;
    }
}
