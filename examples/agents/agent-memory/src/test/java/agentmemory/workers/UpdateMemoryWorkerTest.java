package agentmemory.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateMemoryWorkerTest {

    private final UpdateMemoryWorker worker = new UpdateMemoryWorker();

    @Test
    void taskDefName() {
        assertEquals("am_update_memory", worker.getTaskDefName());
    }

    @Test
    void returnsMemorySnapshot() {
        Task task = taskWith(Map.of("userId", "user-42", "userMessage", "Hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> snapshot =
                (Map<String, Object>) result.getOutputData().get("memorySnapshot");
        assertNotNull(snapshot);
        assertEquals(6, snapshot.get("totalEntries"));
        assertEquals(5, snapshot.get("factsStored"));
        assertEquals("2025-01-15T10:00:00Z", snapshot.get("lastUpdated"));
    }

    @Test
    void returnsMemorySize() {
        Task task = taskWith(Map.of("userId", "user-42", "userMessage", "Hello"));
        TaskResult result = worker.execute(task);

        assertEquals(6, result.getOutputData().get("memorySize"));
    }

    @Test
    void memorySizeMatchesTotalEntries() {
        Task task = taskWith(Map.of("userId", "user-42"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> snapshot =
                (Map<String, Object>) result.getOutputData().get("memorySnapshot");
        int totalEntries = (int) snapshot.get("totalEntries");
        int memorySize = (int) result.getOutputData().get("memorySize");
        assertEquals(totalEntries, memorySize);
    }

    @Test
    void snapshotContainsAllExpectedFields() {
        Task task = taskWith(Map.of("userId", "user-42"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> snapshot =
                (Map<String, Object>) result.getOutputData().get("memorySnapshot");
        assertEquals(3, snapshot.size());
        assertTrue(snapshot.containsKey("totalEntries"));
        assertTrue(snapshot.containsKey("factsStored"));
        assertTrue(snapshot.containsKey("lastUpdated"));
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("memorySnapshot"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("memorySnapshot"));
    }

    @Test
    void handlesBlankUserId() {
        Task task = taskWith(Map.of("userId", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("memorySnapshot"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
