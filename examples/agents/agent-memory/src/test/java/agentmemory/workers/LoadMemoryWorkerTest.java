package agentmemory.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadMemoryWorkerTest {

    private final LoadMemoryWorker worker = new LoadMemoryWorker();

    @Test
    void taskDefName() {
        assertEquals("am_load_memory", worker.getTaskDefName());
    }

    @Test
    void returnsConversationHistory() {
        Task task = taskWith(Map.of("userId", "user-42", "userMessage", "Hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history =
                (List<Map<String, String>>) result.getOutputData().get("conversationHistory");
        assertNotNull(history);
        assertEquals(4, history.size());
    }

    @Test
    void conversationEntriesHaveRoleContentTimestamp() {
        Task task = taskWith(Map.of("userId", "user-42", "userMessage", "Hello"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history =
                (List<Map<String, String>>) result.getOutputData().get("conversationHistory");
        for (Map<String, String> entry : history) {
            assertTrue(entry.containsKey("role"));
            assertTrue(entry.containsKey("content"));
            assertTrue(entry.containsKey("timestamp"));
        }
    }

    @Test
    void conversationAlternatesRoles() {
        Task task = taskWith(Map.of("userId", "user-42", "userMessage", "Hello"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history =
                (List<Map<String, String>>) result.getOutputData().get("conversationHistory");
        assertEquals("user", history.get(0).get("role"));
        assertEquals("assistant", history.get(1).get("role"));
        assertEquals("user", history.get(2).get("role"));
        assertEquals("assistant", history.get(3).get("role"));
    }

    @Test
    void returnsUserProfile() {
        Task task = taskWith(Map.of("userId", "user-42", "userMessage", "Hello"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> profile =
                (Map<String, Object>) result.getOutputData().get("userProfile");
        assertNotNull(profile);
        assertEquals("user-42", profile.get("name"));
        assertEquals("intermediate", profile.get("expertise"));
        assertNotNull(profile.get("interests"));
        assertEquals("2025-01-15T09:01:10Z", profile.get("lastSeen"));
    }

    @Test
    void returnsMemoryEntries() {
        Task task = taskWith(Map.of("userId", "user-42", "userMessage", "Hello"));
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("memoryEntries"));
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", null);
        input.put("userMessage", "Hello");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("conversationHistory"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("conversationHistory"));
        assertNotNull(result.getOutputData().get("userProfile"));
    }

    @Test
    void handlesBlankUserId() {
        Task task = taskWith(Map.of("userId", "   ", "userMessage", "Hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> profile =
                (Map<String, Object>) result.getOutputData().get("userProfile");
        assertEquals("unknown", profile.get("name"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
