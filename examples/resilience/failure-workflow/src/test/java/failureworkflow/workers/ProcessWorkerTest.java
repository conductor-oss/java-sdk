package failureworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessWorkerTest {

    private final ProcessWorker worker = new ProcessWorker();

    @Test
    void taskDefName() {
        assertEquals("fw_process", worker.getTaskDefName());
    }

    @Test
    void succeedsWhenShouldFailIsFalse() {
        Task task = taskWith(Map.of("shouldFail", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SUCCESS", result.getOutputData().get("status"));
        assertEquals("Processing completed successfully", result.getOutputData().get("message"));
    }

    @Test
    void failsWhenShouldFailIsTrue() {
        Task task = taskWith(Map.of("shouldFail", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("FAILED", result.getOutputData().get("status"));
        assertEquals("Processing failed as requested", result.getOutputData().get("message"));
        assertEquals("Intentional failure: shouldFail was true", result.getReasonForIncompletion());
    }

    @Test
    void failsWhenShouldFailIsStringTrue() {
        Task task = taskWith(Map.of("shouldFail", "true"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("FAILED", result.getOutputData().get("status"));
    }

    @Test
    void succeedsWhenShouldFailIsStringFalse() {
        Task task = taskWith(Map.of("shouldFail", "false"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SUCCESS", result.getOutputData().get("status"));
    }

    @Test
    void succeedsWhenShouldFailIsMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SUCCESS", result.getOutputData().get("status"));
    }

    @Test
    void succeedsWhenShouldFailIsNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("shouldFail", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SUCCESS", result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
