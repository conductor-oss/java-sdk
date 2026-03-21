package patchmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyPatchTest {

    private final VerifyPatch worker = new VerifyPatch();

    @Test
    void taskDefName() {
        assertEquals("pm_verify_patch", worker.getTaskDefName());
    }

    @Test
    void verifiesAllHostsHealthy() {
        Task task = taskWith(Map.of("hostsPatched", 24));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals(true, result.getOutputData().get("allHealthy"));
        assertEquals(24, result.getOutputData().get("hostsVerified"));
    }

    @Test
    void verifiesDifferentHostCount() {
        Task task = taskWith(Map.of("hostsPatched", 16));
        TaskResult result = worker.execute(task);

        assertEquals(16, result.getOutputData().get("hostsVerified"));
    }

    @Test
    void completedAtIsDeterministic() {
        Task task = taskWith(Map.of("hostsPatched", 24));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-14T10:00:00Z", result.getOutputData().get("completedAt"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("verify_patchData", null);
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(24, result.getOutputData().get("hostsVerified"));
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
    void allHealthyIsTrue() {
        Task task = taskWith(Map.of("hostsPatched", 10));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("allHealthy"));
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of("hostsPatched", 24));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("verified"));
        assertNotNull(result.getOutputData().get("allHealthy"));
        assertNotNull(result.getOutputData().get("hostsVerified"));
        assertNotNull(result.getOutputData().get("completedAt"));
    }

    private Task taskWith(Map<String, Object> dataMap) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("verify_patchData", dataMap);
        task.setInputData(input);
        return task;
    }
}
