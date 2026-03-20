package mistralai.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MistralChatWorkerTest {

    private final MistralChatWorker worker = new MistralChatWorker();

    @Test
    void taskDefName() {
        assertEquals("mistral_chat", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsDeterministicChatResponse() {
        Task task = taskWith(new HashMap<>(Map.of(
                "requestBody", Map.of("model", "mistral-large-latest")
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> chatResponse = (Map<String, Object>) result.getOutputData().get("chatResponse");
        assertNotNull(chatResponse);

        // In simulated mode, verify fixed response fields
        if (System.getenv("MISTRAL_API_KEY") == null || System.getenv("MISTRAL_API_KEY").isBlank()) {
            assertEquals("cmpl-mst-abc123xyz", chatResponse.get("id"));
            assertEquals("chat.completion", chatResponse.get("object"));
            assertEquals("mistral-large-latest", chatResponse.get("model"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void responseContainsChoiceWithAssistantMessage() {
        Task task = taskWith(new HashMap<>(Map.of(
                "requestBody", Map.of("model", "mistral-large-latest")
        )));

        TaskResult result = worker.execute(task);

        Map<String, Object> chatResponse = (Map<String, Object>) result.getOutputData().get("chatResponse");
        List<Map<String, Object>> choices = (List<Map<String, Object>>) chatResponse.get("choices");
        assertNotNull(choices);
        assertFalse(choices.isEmpty());

        Map<String, Object> choice = choices.get(0);
        assertEquals(0, choice.get("index"));
        assertEquals("stop", choice.get("finish_reason"));

        Map<String, Object> message = (Map<String, Object>) choice.get("message");
        assertEquals("assistant", message.get("role"));
        // In simulated mode, content starts with [SIMULATED]
        if (System.getenv("MISTRAL_API_KEY") == null || System.getenv("MISTRAL_API_KEY").isBlank()) {
            assertTrue(message.get("content").toString().contains("Key contract analysis"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void responseContainsUsageStats() {
        Task task = taskWith(new HashMap<>(Map.of(
                "requestBody", Map.of("model", "mistral-large-latest")
        )));

        TaskResult result = worker.execute(task);

        Map<String, Object> chatResponse = (Map<String, Object>) result.getOutputData().get("chatResponse");
        Map<String, Object> usage = (Map<String, Object>) chatResponse.get("usage");

        // In simulated mode, verify fixed token counts
        if (System.getenv("MISTRAL_API_KEY") == null || System.getenv("MISTRAL_API_KEY").isBlank()) {
            assertEquals(156, usage.get("prompt_tokens"));
            assertEquals(134, usage.get("completion_tokens"));
            assertEquals(290, usage.get("total_tokens"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
