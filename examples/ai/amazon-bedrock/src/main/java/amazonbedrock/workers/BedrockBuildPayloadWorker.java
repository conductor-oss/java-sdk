package amazonbedrock.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Builds the Bedrock InvokeModel payload for Claude on Bedrock.
 *
 * Input: prompt, useCase, modelId, region, maxTokens, temperature
 * Output: payload (Anthropic Messages API body), modelId, region
 */
public class BedrockBuildPayloadWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bedrock_build_payload";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().get("prompt");
        String useCase = (String) task.getInputData().get("useCase");
        String modelId = (String) task.getInputData().get("modelId");
        String region = (String) task.getInputData().get("region");
        Object maxTokens = task.getInputData().get("maxTokens");
        Object temperature = task.getInputData().get("temperature");

        // Build Bedrock payload following Claude on Bedrock's Anthropic Messages API format
        Map<String, Object> payload = Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", maxTokens,
                "temperature", temperature,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", "Use case: " + useCase + "\n\n" + prompt
                        )
                )
        );

        System.out.println("  [build] Bedrock payload: modelId=" + modelId + ", region=" + region);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("payload", payload);
        result.getOutputData().put("modelId", modelId);
        result.getOutputData().put("region", region);
        return result;
    }
}
