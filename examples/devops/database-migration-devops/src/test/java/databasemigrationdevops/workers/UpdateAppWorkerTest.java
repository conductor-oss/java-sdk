package databasemigrationdevops.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateAppWorkerTest {

    private final UpdateAppWorker worker = new UpdateAppWorker();

    @Test
    void taskDefName() {
        assertEquals("dbm_update_app", worker.getTaskDefName());
    }

    @Test
    void updatesAppSuccessfully() {
        Task task = taskWith(Map.of("update_appData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("update_app"));
        assertNotNull(result.getOutputData().get("completedAt"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void alwaysReturnsUpdateApp() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("update_app"));
    }

    @Test
    void completedAtIsDeterministic() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-14T00:00:00Z", result.getOutputData().get("completedAt"));
    }

    @Test
    void statusIsCompleted() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("update_app"));
        assertTrue(result.getOutputData().containsKey("completedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
