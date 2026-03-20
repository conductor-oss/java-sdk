package dynamicworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DwValidateWorkerTest {

    private final DwValidateWorker worker = new DwValidateWorker();

    @Test
    void taskDefName() {
        assertEquals("dw_validate", worker.getTaskDefName());
    }

    @Test
    void validatesSuccessfully() {
        Task task = taskWith(Map.of("stepId", "validate", "stepType", "validation", "config", "{\"required\":true}"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("validate_complete", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsStepType() {
        Task task = taskWith(Map.of("stepId", "validate"));
        TaskResult result = worker.execute(task);

        assertEquals("validation", result.getOutputData().get("stepType"));
    }

    @Test
    void handlesNullStepId() {
        Map<String, Object> input = new HashMap<>();
        input.put("stepId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("validate_complete", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullConfig() {
        Map<String, Object> input = new HashMap<>();
        input.put("stepId", "validate");
        input.put("config", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void resultIsDeterministic() {
        Task task = taskWith(Map.of("stepId", "validate"));
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);

        assertEquals(r1.getOutputData().get("result"), r2.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
