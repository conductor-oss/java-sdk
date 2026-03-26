package patchmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestPatchTest {

    private final TestPatch worker = new TestPatch();

    @Test
    void taskDefName() {
        assertEquals("pm_test_patch", worker.getTaskDefName());
    }

    @Test
    void testsPatchSuccessfully() {
        Task task = taskWith(Map.of("patchId", "PATCH-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("tested"));
        assertEquals(0, result.getOutputData().get("regressions"));
        assertEquals(true, result.getOutputData().get("stagingPassed"));
        assertEquals(87, result.getOutputData().get("testsRun"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("test_patchData", null);
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("tested"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void zeroRegressions() {
        Task task = taskWith(Map.of("patchId", "PATCH-002"));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("regressions"));
    }

    @Test
    void stagingAlwaysPasses() {
        Task task = taskWith(Map.of("patchId", "PATCH-003"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("stagingPassed"));
    }

    @Test
    void testsRunCount() {
        Task task = taskWith(Map.of("patchId", "PATCH-004"));
        TaskResult result = worker.execute(task);

        assertEquals(87, result.getOutputData().get("testsRun"));
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of("patchId", "PATCH-005"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("tested"));
        assertNotNull(result.getOutputData().get("regressions"));
        assertNotNull(result.getOutputData().get("stagingPassed"));
        assertNotNull(result.getOutputData().get("testsRun"));
    }

    private Task taskWith(Map<String, Object> dataMap) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("test_patchData", dataMap);
        task.setInputData(input);
        return task;
    }
}
