package systemprompts.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Builds a full prompt by combining a system prompt, few-shot examples,
 * and the user's prompt based on the requested style (formal or casual).
 */
public class SpBuildPromptWorker implements Worker {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, String> SYSTEM_PROMPTS = Map.of(
            "formal", "You are a senior technical architect. Respond with precise, professional language. Use industry terminology.",
            "casual", "You're a friendly dev buddy. Keep it chill and use simple words. Throw in an analogy if it helps."
    );

    private static final Map<String, List<Map<String, String>>> FEW_SHOT_EXAMPLES = Map.of(
            "formal", List.of(
                    Map.of("role", "user", "content", "What is a database?"),
                    Map.of("role", "assistant", "content", "A database is a structured collection of data managed by a DBMS, supporting ACID transactions and optimized query execution.")
            ),
            "casual", List.of(
                    Map.of("role", "user", "content", "What is a database?"),
                    Map.of("role", "assistant", "content", "Think of it like a super organized filing cabinet for your app's data — it keeps everything neat and findable.")
            )
    );

    @Override
    public String getTaskDefName() {
        return "sp_build_prompt";
    }

    @Override
    public TaskResult execute(Task task) {
        String userPrompt = (String) task.getInputData().get("userPrompt");
        String style = (String) task.getInputData().get("style");

        if (userPrompt == null || userPrompt.isBlank()) {
            userPrompt = "Explain Conductor";
        }
        if (style == null || style.isBlank()) {
            style = "formal";
        }

        String systemPrompt = SYSTEM_PROMPTS.getOrDefault(style, SYSTEM_PROMPTS.get("formal"));
        List<Map<String, String>> fewShot = FEW_SHOT_EXAMPLES.getOrDefault(style, FEW_SHOT_EXAMPLES.get("formal"));

        Map<String, Object> promptData = Map.of(
                "system", systemPrompt,
                "fewShot", fewShot,
                "user", userPrompt
        );

        String fullPrompt;
        try {
            fullPrompt = MAPPER.writeValueAsString(promptData);
        } catch (JsonProcessingException e) {
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Failed to serialize prompt: " + e.getMessage());
            return result;
        }

        System.out.println("  [sp_build_prompt] Built " + style + " prompt for: " + userPrompt);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("fullPrompt", fullPrompt);
        result.getOutputData().put("style", style);
        return result;
    }
}
