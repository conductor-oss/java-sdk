package networkautomation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyConnectivityTest {

    private final VerifyConnectivity worker = new VerifyConnectivity();

    @Test
    void taskDefName() {
        assertEquals("na_verify_connectivity", worker.getTaskDefName());
    }

    @Test
    void verifiesConnectivity() {
        Task task = taskWith(Map.of("devicesConfigured", 12));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals(true, result.getOutputData().get("allTestsPassed"));
        assertEquals(12, result.getOutputData().get("devicesVerified"));
    }

    @Test
    void verifiesDifferentDeviceCount() {
        Task task = taskWith(Map.of("devicesConfigured", 6));
        TaskResult result = worker.execute(task);

        assertEquals(6, result.getOutputData().get("devicesVerified"));
    }

    @Test
    void completedAtIsDeterministic() {
        Task task = taskWith(Map.of("devicesConfigured", 12));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-14T10:00:00Z", result.getOutputData().get("completedAt"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("verify_connectivityData", null);
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
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
    void allTestsPassedIsTrue() {
        Task task = taskWith(Map.of("devicesConfigured", 8));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("allTestsPassed"));
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of("devicesConfigured", 12));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("verified"));
        assertNotNull(result.getOutputData().get("allTestsPassed"));
        assertNotNull(result.getOutputData().get("devicesVerified"));
        assertNotNull(result.getOutputData().get("completedAt"));
    }

    private Task taskWith(Map<String, Object> dataMap) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("verify_connectivityData", dataMap);
        task.setInputData(input);
        return task;
    }
}
