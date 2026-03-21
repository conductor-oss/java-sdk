package amazonbedrock.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BedrockBuildPayloadWorkerTest {

    private final BedrockBuildPayloadWorker worker = new BedrockBuildPayloadWorker();

    @Test
    void taskDefName() {
        assertEquals("bedrock_build_payload", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsPayloadWithCorrectStructure() {
        Task task = taskWith(Map.of(
                "prompt", "Classify this ticket",
                "useCase", "Support triage",
                "modelId", "anthropic.claude-3-sonnet-20240229-v1:0",
                "region", "us-east-1",
                "maxTokens", 512,
                "temperature", 0.5
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        // Verify payload structure
        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("payload");
        assertNotNull(payload);
        assertEquals("bedrock-2023-05-31", payload.get("anthropic_version"));
        assertEquals(512, payload.get("max_tokens"));
        assertEquals(0.5, payload.get("temperature"));

        // Verify messages
        List<Map<String, Object>> messages = (List<Map<String, Object>>) payload.get("messages");
        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).get("role"));
        String content = (String) messages.get(0).get("content");
        assertTrue(content.startsWith("Use case: Support triage"));
        assertTrue(content.contains("Classify this ticket"));
    }

    @Test
    void outputContainsModelIdAndRegion() {
        Task task = taskWith(Map.of(
                "prompt", "Test prompt",
                "useCase", "Test use case",
                "modelId", "anthropic.claude-3-sonnet-20240229-v1:0",
                "region", "us-west-2",
                "maxTokens", 256,
                "temperature", 0.7
        ));

        TaskResult result = worker.execute(task);

        assertEquals("anthropic.claude-3-sonnet-20240229-v1:0", result.getOutputData().get("modelId"));
        assertEquals("us-west-2", result.getOutputData().get("region"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void messageContentCombinesUseCaseAndPrompt() {
        Task task = taskWith(Map.of(
                "prompt", "My prompt text",
                "useCase", "My use case",
                "modelId", "model-id",
                "region", "us-east-1",
                "maxTokens", 100,
                "temperature", 0.0
        ));

        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("payload");
        List<Map<String, Object>> messages = (List<Map<String, Object>>) payload.get("messages");
        String content = (String) messages.get(0).get("content");
        assertEquals("Use case: My use case\n\nMy prompt text", content);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
