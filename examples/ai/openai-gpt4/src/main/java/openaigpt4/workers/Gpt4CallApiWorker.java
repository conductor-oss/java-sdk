package openaigpt4.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls the OpenAI GPT-4 chat completion API.
 *
 * Requires CONDUCTOR_OPENAI_API_KEY to be set.
 */
public class Gpt4CallApiWorker implements Worker {

    private static final String DEFAULT_MODEL = Gpt4BuildRequestWorker.configuredDefaultModel();

    private final OpenAiService openAiService;

    public Gpt4CallApiWorker() {
        String apiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_OPENAI_API_KEY environment variable to run this worker");
        }
        this.openAiService = new OpenAiService(apiKey);
    }

    /** Package-private constructor for testing with an explicit OpenAiService. */
    Gpt4CallApiWorker(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @Override
    public String getTaskDefName() {
        return "gpt4_call_api";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        try {
            Map<String, Object> requestBody = (Map<String, Object>) task.getInputData().get("requestBody");

            // Extract parameters from the request body built by Gpt4BuildRequestWorker
            String model = (String) requestBody.getOrDefault("model", DEFAULT_MODEL);
            List<Map<String, Object>> messagesInput = (List<Map<String, Object>>) requestBody.get("messages");
            Number temperature = (Number) requestBody.getOrDefault("temperature", 0.7);
            Number maxTokens = (Number) requestBody.getOrDefault("max_tokens", 1024);

            // Build ChatMessage list from the request body
            List<ChatMessage> chatMessages = new ArrayList<>();
            if (messagesInput != null) {
                for (Map<String, Object> msg : messagesInput) {
                    String role = (String) msg.get("role");
                    String content = (String) msg.get("content");
                    chatMessages.add(new ChatMessage(role, content));
                }
            }

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(chatMessages)
                    .temperature(temperature.doubleValue())
                    .maxTokens(maxTokens.intValue())
                    .build();

            ChatCompletionResult response = openAiService.createChatCompletion(request);

            // Convert the SDK response into the same map structure the extract worker expects
            String responseContent = response.getChoices().get(0).getMessage().getContent();
            String finishReason = response.getChoices().get(0).getFinishReason();

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "assistant");
            message.put("content", responseContent);

            Map<String, Object> choice = new LinkedHashMap<>();
            choice.put("index", 0);
            choice.put("message", message);
            choice.put("finish_reason", finishReason);

            Map<String, Object> usage = new LinkedHashMap<>();
            usage.put("prompt_tokens", response.getUsage().getPromptTokens());
            usage.put("completion_tokens", response.getUsage().getCompletionTokens());
            usage.put("total_tokens", response.getUsage().getTotalTokens());

            Map<String, Object> apiResponse = new LinkedHashMap<>();
            apiResponse.put("id", response.getId());
            apiResponse.put("object", "chat.completion");
            apiResponse.put("model", response.getModel());
            apiResponse.put("choices", List.of(choice));
            apiResponse.put("usage", usage);

            result.getOutputData().put("apiResponse", apiResponse);

            String preview = responseContent.substring(0, Math.min(80, responseContent.length()));
            System.out.println("  [gpt4_call_api worker] Response from OpenAI API: " + preview + "...");

        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("OpenAI API call failed: " + e.getMessage());
            System.out.println("  [gpt4_call_api worker] ERROR: " + e.getMessage());
        }

        return result;
    }
}
