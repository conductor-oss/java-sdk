package multiregiondeploy.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyGlobalWorkerTest {

    private final VerifyGlobalWorker worker = new VerifyGlobalWorker();

    @Test
    void taskDefName() {
        assertEquals("mrd_verify_global", worker.getTaskDefName());
    }

    @Test
    void verifiesGlobalSuccessfully() {
        Task task = taskWith(Map.of("verify_globalData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verify_global"));
        assertNotNull(result.getOutputData().get("completedAt"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void alwaysReturnsVerifyGlobal() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("verify_global"));
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

        assertTrue(result.getOutputData().containsKey("verify_global"));
        assertTrue(result.getOutputData().containsKey("completedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
