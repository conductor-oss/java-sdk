package conversationalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SaveHistoryWorkerTest {

    private final SaveHistoryWorker worker = new SaveHistoryWorker();

    @BeforeEach
    void clearSessionStore() {
        LoadHistoryWorker.SESSION_STORE.clear();
    }

    @Test
    void taskDefName() {
        assertEquals("crag_save_history", worker.getTaskDefName());
    }

    @Test
    void savesFirstTurn() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sessionId", "session-1",
                "userMessage", "Hello",
                "assistantMessage", "Hi there"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("turnNumber"));
        assertEquals(true, result.getOutputData().get("saved"));

        List<Map<String, String>> stored = LoadHistoryWorker.SESSION_STORE.get("session-1");
        assertNotNull(stored);
        assertEquals(1, stored.size());
        assertEquals("Hello", stored.get(0).get("user"));
        assertEquals("Hi there", stored.get(0).get("assistant"));
    }

    @Test
    void appendsToExistingHistory() {
        List<Map<String, String>> existing = new ArrayList<>();
        existing.add(new HashMap<>(Map.of("user", "First", "assistant", "Response 1")));

        Task task = taskWith(new HashMap<>(Map.of(
                "sessionId", "session-2",
                "userMessage", "Second",
                "assistantMessage", "Response 2",
                "history", existing
        )));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("turnNumber"));
        assertEquals(true, result.getOutputData().get("saved"));

        List<Map<String, String>> stored = LoadHistoryWorker.SESSION_STORE.get("session-2");
        assertEquals(2, stored.size());
        assertEquals("First", stored.get(0).get("user"));
        assertEquals("Second", stored.get(1).get("user"));
    }

    @Test
    void handlesNullHistory() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sessionId", "session-3",
                "userMessage", "Hello",
                "assistantMessage", "Hi"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("turnNumber"));
        assertEquals(true, result.getOutputData().get("saved"));
    }

    @Test
    void handlesEmptySessionId() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sessionId", "",
                "userMessage", "Test",
                "assistantMessage", "Response"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // Falls back to "default" session
        assertNotNull(LoadHistoryWorker.SESSION_STORE.get("default"));
    }

    @Test
    void handlesMissingSessionId() {
        Task task = taskWith(new HashMap<>(Map.of(
                "userMessage", "Test",
                "assistantMessage", "Response"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(LoadHistoryWorker.SESSION_STORE.get("default"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
