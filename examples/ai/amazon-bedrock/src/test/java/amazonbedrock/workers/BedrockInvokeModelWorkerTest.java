package amazonbedrock.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BedrockInvokeModelWorkerTest {

    private final BedrockInvokeModelWorker worker = new BedrockInvokeModelWorker();

    @Test
    void taskDefName() {
        assertEquals("bedrock_invoke_model", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsSimulatedBedrockResponse() {
        Task task = taskWith(Map.of(
                "modelId", "anthropic.claude-3-sonnet-20240229-v1:0",
                "payload", Map.of("anthropic_version", "bedrock-2023-05-31"),
                "region", "us-east-1"
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        // Verify response body structure
        Map<String, Object> responseBody = (Map<String, Object>) result.getOutputData().get("responseBody");
        assertNotNull(responseBody);
        assertEquals("msg_bdrk_01ABcDeFgHiJ", responseBody.get("id"));
        assertEquals("message", responseBody.get("type"));
        assertEquals("assistant", responseBody.get("role"));
        assertEquals("end_turn", responseBody.get("stop_reason"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void responseContainsClassificationText() {
        Task task = taskWith(Map.of(
                "modelId", "anthropic.claude-3-sonnet-20240229-v1:0",
                "payload", Map.of(),
                "region", "us-east-1"
        ));

        TaskResult result = worker.execute(task);

        Map<String, Object> responseBody = (Map<String, Object>) result.getOutputData().get("responseBody");
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
        assertEquals(1, content.size());
        assertEquals("text", content.get(0).get("type"));
        String text = (String) content.get(0).get("text");
        assertTrue(text.contains("Classification: URGENT"));
        assertTrue(text.contains("Confidence: 94%"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void responseContainsUsageTokens() {
        Task task = taskWith(Map.of(
                "modelId", "model-id",
                "payload", Map.of(),
                "region", "us-east-1"
        ));

        TaskResult result = worker.execute(task);

        Map<String, Object> responseBody = (Map<String, Object>) result.getOutputData().get("responseBody");
        Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
        assertEquals(67, usage.get("input_tokens"));
        assertEquals(89, usage.get("output_tokens"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void responseContainsMetrics() {
        Task task = taskWith(Map.of(
                "modelId", "model-id",
                "payload", Map.of(),
                "region", "us-east-1"
        ));

        TaskResult result = worker.execute(task);

        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("metrics");
        assertNotNull(metrics);
        assertEquals(1850, metrics.get("latencyMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
