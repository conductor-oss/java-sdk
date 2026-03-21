package mistralai.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Composes a Mistral chat completion request body from workflow inputs.
 *
 * Inputs: document, question, model, temperature, maxTokens, safePrompt
 * Output: requestBody (the full JSON-ready request object)
 */
public class MistralComposeRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mistral_compose_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String document = (String) task.getInputData().get("document");
        String question = (String) task.getInputData().get("question");
        String model = (String) task.getInputData().get("model");
        Object temperature = task.getInputData().get("temperature");
        Object maxTokens = task.getInputData().get("maxTokens");
        Object safePrompt = task.getInputData().get("safePrompt");

        if (model == null || model.isBlank()) {
            model = "mistral-large-latest";
        }
        if (temperature == null) {
            temperature = 0.7;
        }
        if (maxTokens == null) {
            maxTokens = 1024;
        }
        if (safePrompt == null) {
            safePrompt = false;
        }

        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "You are a legal document analyst. Analyze the provided document and answer questions about it accurately and concisely.");

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", "Document:\n" + document + "\n\nQuestion: " + question);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(systemMessage, userMessage));
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("safe_prompt", safePrompt);

        System.out.println("  [mistral_compose_request] Composed request for model: " + model);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestBody", requestBody);
        return result;
    }
}
