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

class LoadHistoryWorkerTest {

    private final LoadHistoryWorker worker = new LoadHistoryWorker();

    @BeforeEach
    void clearSessionStore() {
        LoadHistoryWorker.SESSION_STORE.clear();
    }

    @Test
    void taskDefName() {
        assertEquals("crag_load_history", worker.getTaskDefName());
    }

    @Test
    void returnsEmptyHistoryForNewSession() {
        Task task = taskWith(new HashMap<>(Map.of("sessionId", "new-session")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) result.getOutputData().get("history");
        assertNotNull(history);
        assertTrue(history.isEmpty());
        assertEquals(0, result.getOutputData().get("turnCount"));
    }

    @Test
    void returnsExistingHistoryForKnownSession() {
        List<Map<String, String>> existing = new ArrayList<>();
        existing.add(new HashMap<>(Map.of("user", "Hello", "assistant", "Hi there")));
        LoadHistoryWorker.SESSION_STORE.put("known-session", existing);

        Task task = taskWith(new HashMap<>(Map.of("sessionId", "known-session")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) result.getOutputData().get("history");
        assertEquals(1, history.size());
        assertEquals("Hello", history.get(0).get("user"));
        assertEquals(1, result.getOutputData().get("turnCount"));
    }

    @Test
    void handlesEmptySessionId() {
        Task task = taskWith(new HashMap<>(Map.of("sessionId", "")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("history"));
    }

    @Test
    void handlesMissingSessionId() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("history"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
