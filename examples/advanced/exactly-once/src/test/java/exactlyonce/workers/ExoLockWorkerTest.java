package exactlyonce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExoLockWorkerTest {

    private final ExoLockWorker worker = new ExoLockWorker();

    @BeforeEach
    void setUp() {
        ExoLockWorker.clearLocks();
    }

    @Test
    void taskDefName() {
        assertEquals("exo_lock", worker.getTaskDefName());
    }

    @Test
    void acquiresLockOnFirstAttempt() {
        Task task = taskWith(Map.of("resourceKey", "res-1", "ttlSeconds", 30));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acquired"));
        assertNotNull(result.getOutputData().get("lockToken"));
    }

    @Test
    void failsToAcquireWhenAlreadyLocked() {
        // First lock succeeds
        Task task1 = taskWith(Map.of("resourceKey", "res-2"));
        TaskResult r1 = worker.execute(task1);
        assertEquals(true, r1.getOutputData().get("acquired"));

        // Second lock on same key fails
        Task task2 = taskWith(Map.of("resourceKey", "res-2"));
        TaskResult r2 = worker.execute(task2);
        assertEquals(false, r2.getOutputData().get("acquired"));
    }

    @Test
    void differentKeysCanBothLock() {
        Task task1 = taskWith(Map.of("resourceKey", "res-A"));
        Task task2 = taskWith(Map.of("resourceKey", "res-B"));

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(true, r1.getOutputData().get("acquired"));
        assertEquals(true, r2.getOutputData().get("acquired"));
    }

    @Test
    void lockTokenStartsWithPrefix() {
        Task task = taskWith(Map.of("resourceKey", "res-3"));
        TaskResult result = worker.execute(task);

        String token = (String) result.getOutputData().get("lockToken");
        assertTrue(token.startsWith("lock-"), "Token should start with 'lock-', got: " + token);
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("lockToken"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
