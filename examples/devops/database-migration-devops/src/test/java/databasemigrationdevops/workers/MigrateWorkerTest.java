package databasemigrationdevops.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MigrateWorkerTest {

    private final MigrateWorker worker = new MigrateWorker();

    @Test
    void taskDefName() {
        assertEquals("dbm_migrate", worker.getTaskDefName());
    }

    @Test
    void migratesSuccessfully() {
        Task task = taskWith(Map.of("migrateData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("migrate"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsAlterStatements() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("alterStatements"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void alwaysReturnsMigrate() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("migrate"));
    }

    @Test
    void alwaysReturnsProcessed() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("migrate"));
        assertTrue(result.getOutputData().containsKey("processed"));
        assertTrue(result.getOutputData().containsKey("alterStatements"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
