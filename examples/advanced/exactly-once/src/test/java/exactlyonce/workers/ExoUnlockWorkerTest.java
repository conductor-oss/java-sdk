package exactlyonce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExoUnlockWorkerTest {

    private final ExoUnlockWorker worker = new ExoUnlockWorker();

    @BeforeEach
    void setUp() {
        ExoLockWorker.clearLocks();
    }

    @Test
    void taskDefName() {
        assertEquals("exo_unlock", worker.getTaskDefName());
    }

    @Test
    void unlocksWithCorrectToken() {
        ExoLockWorker.LOCKS.put("res-1", "tok-abc");

        Task task = taskWith(Map.of("resourceKey", "res-1", "lockToken", "tok-abc"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("unlocked"));
        assertFalse(ExoLockWorker.LOCKS.containsKey("res-1"));
    }

    @Test
    void failsToUnlockWithWrongToken() {
        ExoLockWorker.LOCKS.put("res-2", "tok-real");

        Task task = taskWith(Map.of("resourceKey", "res-2", "lockToken", "tok-wrong"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("unlocked"));
        assertTrue(ExoLockWorker.LOCKS.containsKey("res-2"), "Lock should remain");
    }

    @Test
    void unlockAlreadyUnlockedSucceeds() {
        Task task = taskWith(Map.of("resourceKey", "res-3", "lockToken", "tok-x"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("unlocked"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("unlocked"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
