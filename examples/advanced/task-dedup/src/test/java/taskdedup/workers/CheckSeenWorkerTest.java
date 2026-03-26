package taskdedup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckSeenWorkerTest {

    private final CheckSeenWorker worker = new CheckSeenWorker();

    @Test
    void taskDefName() {
        assertEquals("tdd_check_seen", worker.getTaskDefName());
    }

    @Test
    void returnsNewStatusForFirstSeen() {
        Task task = taskWith(Map.of("hash", "sha256:abc123", "cacheEnabled", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("new", result.getOutputData().get("status"));
    }

    @Test
    void cachedResultIsNullForNewItems() {
        Task task = taskWith(Map.of("hash", "sha256:def456", "cacheEnabled", true));
        TaskResult result = worker.execute(task);

        assertNull(result.getOutputData().get("cachedResult"));
    }

    @Test
    void statusOutputIsPresent() {
        Task task = taskWith(Map.of("hash", "sha256:ghi789", "cacheEnabled", false));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("status"));
    }

    @Test
    void handlesNullHash() {
        Map<String, Object> input = new HashMap<>();
        input.put("hash", null);
        input.put("cacheEnabled", true);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("new", result.getOutputData().get("status"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesCacheDisabled() {
        Task task = taskWith(Map.of("hash", "sha256:xyz999", "cacheEnabled", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("new", result.getOutputData().get("status"));
    }

    @Test
    void outputContainsBothFields() {
        Task task = taskWith(Map.of("hash", "sha256:test000", "cacheEnabled", true));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("status"));
        assertTrue(result.getOutputData().containsKey("cachedResult"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
