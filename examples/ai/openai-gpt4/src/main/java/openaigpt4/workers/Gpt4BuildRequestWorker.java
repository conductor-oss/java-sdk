package openaigpt4.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds an OpenAI chat completion request body from workflow input parameters.
 */
public class Gpt4BuildRequestWorker implements Worker {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    @Override
    public String getTaskDefName() {
        return "gpt4_build_request";
    }

    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> input = task.getInputData();

        String prompt = (String) input.getOrDefault("prompt", "");
        String systemMessage = (String) input.getOrDefault("systemMessage", "You are a helpful assistant.");
        String model = (String) input.getOrDefault("model", configuredDefaultModel());
        Number temperature = (Number) input.getOrDefault("temperature", 0.7);
        Number topP = (Number) input.getOrDefault("top_p", 1.0);
        Number maxTokens = (Number) input.getOrDefault("max_tokens", 1024);
        Number frequencyPenalty = (Number) input.getOrDefault("frequency_penalty", 0.0);
        Number presencePenalty = (Number) input.getOrDefault("presence_penalty", 0.0);

        Map<String, Object> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemMessage);

        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        List<Map<String, Object>> messages = List.of(systemMsg, userMsg);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);
        requestBody.put("top_p", topP);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("frequency_penalty", frequencyPenalty);
        requestBody.put("presence_penalty", presencePenalty);

        System.out.println("  [gpt4_build_request worker] Built request for model: " + model);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestBody", requestBody);
        return result;
    }

    public static String configuredDefaultModel() {
        String configured = System.getenv("OPENAI_CHAT_MODEL");
        return configured != null && !configured.isBlank() ? configured : DEFAULT_MODEL;
    }
}
