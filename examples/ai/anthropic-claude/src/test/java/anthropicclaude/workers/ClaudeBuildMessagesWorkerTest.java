package anthropicclaude.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeBuildMessagesWorkerTest {

    private final ClaudeBuildMessagesWorker worker = new ClaudeBuildMessagesWorker();

    @Test
    void taskDefName() {
        assertEquals("claude_build_messages", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsRequestBodyWithCorrectStructure() {
        Task task = taskWith(new HashMap<>(Map.of(
                "userMessage", "Audit the auth module",
                "systemPrompt", "You are a security engineer",
                "model", "claude-sonnet-4-20250514",
                "max_tokens", 1024,
                "temperature", 0.5
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        assertNotNull(requestBody);
        assertEquals("claude-sonnet-4-20250514", requestBody.get("model"));
        assertEquals(1024, requestBody.get("max_tokens"));
        assertEquals(0.5, requestBody.get("temperature"));
        assertEquals("You are a security engineer", requestBody.get("system"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void messagesContainUserRoleWithContent() {
        Task task = taskWith(new HashMap<>(Map.of(
                "userMessage", "Hello Claude",
                "systemPrompt", "Be helpful",
                "model", "claude-sonnet-4-20250514",
                "max_tokens", 512,
                "temperature", 0.7
        )));

        TaskResult result = worker.execute(task);

        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        List<Map<String, Object>> messages = (List<Map<String, Object>>) requestBody.get("messages");
        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).get("role"));
        assertEquals("Hello Claude", messages.get(0).get("content"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void systemPromptIsSeparateFromMessages() {
        Task task = taskWith(new HashMap<>(Map.of(
                "userMessage", "Test message",
                "systemPrompt", "System instructions here",
                "model", "claude-sonnet-4-20250514",
                "max_tokens", 256,
                "temperature", 0.0
        )));

        TaskResult result = worker.execute(task);

        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        assertEquals("System instructions here", requestBody.get("system"));

        // Verify system prompt is NOT in the messages array
        List<Map<String, Object>> messages = (List<Map<String, Object>>) requestBody.get("messages");
        for (Map<String, Object> msg : messages) {
            assertNotEquals("system", msg.get("role"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void outputContainsRequestBodyKey() {
        Task task = taskWith(new HashMap<>(Map.of(
                "userMessage", "Test",
                "systemPrompt", "Prompt",
                "model", "claude-sonnet-4-20250514",
                "max_tokens", 100,
                "temperature", 0.5
        )));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("requestBody"));
        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        assertTrue(requestBody.containsKey("model"));
        assertTrue(requestBody.containsKey("max_tokens"));
        assertTrue(requestBody.containsKey("temperature"));
        assertTrue(requestBody.containsKey("system"));
        assertTrue(requestBody.containsKey("messages"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
