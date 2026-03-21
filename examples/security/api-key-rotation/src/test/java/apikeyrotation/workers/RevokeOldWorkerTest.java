package apikeyrotation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RevokeOldWorkerTest {

    private final RevokeOldWorker worker = new RevokeOldWorker();

    @Test
    void taskDefName() {
        assertEquals("akr_revoke_old", worker.getTaskDefName());
    }

    @Test
    void revokesOldKey() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("revoke_old"));
        assertNotNull(result.getOutputData().get("completedAt"));
    }

    @Test
    void completedAtIsTimestamp() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        String completedAt = (String) result.getOutputData().get("completedAt");
        assertNotNull(completedAt);
        assertTrue(completedAt.contains("T"), "completedAt should be ISO timestamp");
    }

    @Test
    void outputContainsBothFields() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("revoke_old"));
        assertTrue(result.getOutputData().containsKey("completedAt"));
    }
}
