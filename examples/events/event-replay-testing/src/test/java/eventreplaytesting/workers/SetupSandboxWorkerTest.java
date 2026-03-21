package eventreplaytesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SetupSandboxWorkerTest {

    private final SetupSandboxWorker worker = new SetupSandboxWorker();

    @Test
    void taskDefName() {
        assertEquals("rt_setup_sandbox", worker.getTaskDefName());
    }

    @Test
    void setupsCompletesSuccessfully() {
        Task task = taskWith(Map.of("testSuiteId", "suite-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsDeterministicSandboxId() {
        Task task = taskWith(Map.of("testSuiteId", "suite-002"));
        TaskResult result = worker.execute(task);

        assertEquals("sandbox_fixed_001", result.getOutputData().get("sandboxId"));
    }

    @Test
    void returnsReadyTrue() {
        Task task = taskWith(Map.of("testSuiteId", "suite-003"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void sandboxIdConsistentAcrossCalls() {
        Task task1 = taskWith(Map.of("testSuiteId", "suite-004"));
        Task task2 = taskWith(Map.of("testSuiteId", "suite-005"));
        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("sandboxId"),
                result2.getOutputData().get("sandboxId"));
    }

    @Test
    void handlesNullTestSuiteId() {
        Map<String, Object> input = new HashMap<>();
        input.put("testSuiteId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("sandbox_fixed_001", result.getOutputData().get("sandboxId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void outputContainsBothFields() {
        Task task = taskWith(Map.of("testSuiteId", "suite-006"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("sandboxId"));
        assertTrue(result.getOutputData().containsKey("ready"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
