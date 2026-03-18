package openaigpt4.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Extracts the assistant's response content from the GPT-4 API response.
 */
public class Gpt4ExtractResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gpt4_extract_result";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> apiResponse = (Map<String, Object>) task.getInputData().get("apiResponse");

        String content = "";
        if (apiResponse != null) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) apiResponse.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                if (message != null) {
                    content = (String) message.get("content");
                }
            }
        }

        System.out.println("  [gpt4_extract_result worker] Extracted summary (" + content.length() + " chars)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", content);
        return result;
    }
}
