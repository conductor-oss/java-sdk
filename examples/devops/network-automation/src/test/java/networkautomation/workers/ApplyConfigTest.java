package networkautomation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplyConfigTest {

    private final ApplyConfig worker = new ApplyConfig();

    @Test
    void taskDefName() {
        assertEquals("na_apply_config", worker.getTaskDefName());
    }

    @Test
    void appliesConfigWhenPlanned() {
        Task task = taskWith(Map.of("planned", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("applied"));
        assertEquals(12, result.getOutputData().get("devicesConfigured"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void appliesEvenWhenNotPlanned() {
        Task task = taskWith(Map.of("planned", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("applied"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("apply_configData", null);
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
    void devicesConfiguredIs12() {
        Task task = taskWith(Map.of("planned", true));
        TaskResult result = worker.execute(task);

        assertEquals(12, result.getOutputData().get("devicesConfigured"));
    }

    @Test
    void processedIsTrue() {
        Task task = taskWith(Map.of("planned", true));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of("planned", true));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("applied"));
        assertNotNull(result.getOutputData().get("devicesConfigured"));
        assertNotNull(result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> dataMap) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("apply_configData", dataMap);
        task.setInputData(input);
        return task;
    }
}
