package gitopsworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyStateWorkerTest {

    private final VerifyStateWorker worker = new VerifyStateWorker();

    @Test
    void taskDefName() {
        assertEquals("go_verify_state", worker.getTaskDefName());
    }

    @Test
    void verifiesStateSuccessfully() {
        Task task = taskWith(Map.of("verify_stateData", Map.of("apply_sync", true)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verify_state"));
        assertNotNull(result.getOutputData().get("completedAt"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void alwaysReturnsVerifyState() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("verify_state"));
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

        assertTrue(result.getOutputData().containsKey("verify_state"));
        assertTrue(result.getOutputData().containsKey("completedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
