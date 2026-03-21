package datamasking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DmValidateOutputWorkerTest {

    private final DmValidateOutputWorker worker = new DmValidateOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("dm_validate_output", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("validate_outputData", Map.of("apply_masking", true)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsValidateFlag() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("validate_output"));
    }

    @Test
    void outputContainsCompletedAt() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("completedAt"));
    }

    @Test
    void completedAtIsDeterministic() {
        Task task = taskWith(Map.of());
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);

        assertEquals(r1.getOutputData().get("completedAt"), r2.getOutputData().get("completedAt"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullInput() {
        Map<String, Object> input = new HashMap<>();
        input.put("validate_outputData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputHasTwoEntries() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
