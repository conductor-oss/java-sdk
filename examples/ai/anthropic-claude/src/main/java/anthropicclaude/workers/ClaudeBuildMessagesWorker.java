package anthropicclaude.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Builds the Claude Messages API request body.
 *
 * Claude uses a separate system parameter (not in messages array)
 * and content blocks with type/text structure.
 */
public class ClaudeBuildMessagesWorker implements Worker {

    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";

    @Override
    public String getTaskDefName() {
        return "claude_build_messages";
    }

    @Override
    public TaskResult execute(Task task) {
        String userMessage = (String) task.getInputData().get("userMessage");
        String systemPrompt = (String) task.getInputData().get("systemPrompt");
        Object modelObj = task.getInputData().get("model");
        String model = (modelObj != null && !modelObj.toString().isBlank())
                ? modelObj.toString() : configuredDefaultModel();

        // Explicitly coerce numeric types — Conductor's JSON round-trip can
        // change Integer to Long or Double to BigDecimal, and the Anthropic API
        // rejects malformed numeric fields with HTTP 400.
        int maxTokens = ((Number) task.getInputData().get("max_tokens")).intValue();
        double temperature = ((Number) task.getInputData().get("temperature")).doubleValue();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", temperature,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", userMessage
                        )
                )
        );

        System.out.println("  [build] Claude request: model=" + model + ", system prompt set");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestBody", requestBody);
        return result;
    }

    static String configuredDefaultModel() {
        String configured = System.getenv("ANTHROPIC_MODEL");
        return configured != null && !configured.isBlank() ? configured : DEFAULT_MODEL;
    }
}
