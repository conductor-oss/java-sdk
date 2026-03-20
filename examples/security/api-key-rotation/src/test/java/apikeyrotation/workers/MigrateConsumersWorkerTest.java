package apikeyrotation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MigrateConsumersWorkerTest {

    private final MigrateConsumersWorker worker = new MigrateConsumersWorker();

    @Test
    void taskDefName() {
        assertEquals("akr_migrate_consumers", worker.getTaskDefName());
    }

    @Test
    void migratesSuccessfully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("migrate_consumers"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsMigrateConsumersFlag() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("migrate_consumers"));
    }

    @Test
    void alwaysMarkedAsProcessed() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }
}
