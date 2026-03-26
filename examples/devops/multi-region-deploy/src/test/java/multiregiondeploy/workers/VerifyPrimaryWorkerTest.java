package multiregiondeploy.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyPrimaryWorkerTest {

    private final VerifyPrimaryWorker worker = new VerifyPrimaryWorker();

    @Test
    void taskDefName() {
        assertEquals("mrd_verify_primary", worker.getTaskDefName());
    }

    @Test
    void verifiesPrimarySuccessfully() {
        Task task = taskWith(Map.of("verify_primaryData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verify_primary"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void alwaysReturnsVerifyPrimary() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("verify_primary"));
    }

    @Test
    void alwaysReturnsProcessed() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
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

        assertTrue(result.getOutputData().containsKey("verify_primary"));
        assertTrue(result.getOutputData().containsKey("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
