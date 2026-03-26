package dynamicworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DwTransformWorkerTest {

    private final DwTransformWorker worker = new DwTransformWorker();

    @Test
    void taskDefName() {
        assertEquals("dw_transform", worker.getTaskDefName());
    }

    @Test
    void transformsSuccessfully() {
        Task task = taskWith(Map.of("stepId", "transform", "config", "{\"format\":\"json\"}"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("transform_complete", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsStepType() {
        Task task = taskWith(Map.of("stepId", "transform"));
        TaskResult result = worker.execute(task);

        assertEquals("transformation", result.getOutputData().get("stepType"));
    }

    @Test
    void handlesNullConfig() {
        Map<String, Object> input = new HashMap<>();
        input.put("config", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesPreviousOutput() {
        Task task = taskWith(Map.of("previousOutput", "validate_complete", "config", "{}"));
        TaskResult result = worker.execute(task);

        assertEquals("transform_complete", result.getOutputData().get("result"));
    }

    @Test
    void outputHasBothKeys() {
        Task task = taskWith(Map.of("config", "{}"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("stepType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
