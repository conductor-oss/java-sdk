package apikeyrotation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DualActiveWorkerTest {

    private final DualActiveWorker worker = new DualActiveWorker();

    @Test
    void taskDefName() {
        assertEquals("akr_dual_active", worker.getTaskDefName());
    }

    @Test
    void activatesBothKeys() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("dual_active"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void alwaysCompletes() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsBothExpectedFlags() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("dual_active"));
        assertTrue(result.getOutputData().containsKey("processed"));
    }
}
