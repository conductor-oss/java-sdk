package eventreplaytesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompareResultWorkerTest {

    private final CompareResultWorker worker = new CompareResultWorker();

    @Test
    void taskDefName() {
        assertEquals("rt_compare_result", worker.getTaskDefName());
    }

    @Test
    void matchWhenActualEqualsExpected() {
        Task task = taskWith(Map.of(
                "actual", "processed",
                "expected", "processed",
                "iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("match"));
    }

    @Test
    void noMatchWhenActualDiffersFromExpected() {
        Task task = taskWith(Map.of(
                "actual", "failed",
                "expected", "processed",
                "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("match"));
    }

    @Test
    void outputContainsActualValue() {
        Task task = taskWith(Map.of(
                "actual", "processed",
                "expected", "processed",
                "iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals("processed", result.getOutputData().get("actual"));
    }

    @Test
    void outputContainsExpectedValue() {
        Task task = taskWith(Map.of(
                "actual", "processed",
                "expected", "processed",
                "iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals("processed", result.getOutputData().get("expected"));
    }

    @Test
    void handlesNullActual() {
        Map<String, Object> input = new HashMap<>();
        input.put("actual", null);
        input.put("expected", "processed");
        input.put("iteration", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("match"));
        assertEquals("unknown", result.getOutputData().get("actual"));
    }

    @Test
    void handlesNullExpected() {
        Map<String, Object> input = new HashMap<>();
        input.put("actual", "processed");
        input.put("expected", null);
        input.put("iteration", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("match"));
        assertEquals("unknown", result.getOutputData().get("expected"));
    }

    @Test
    void handlesBothNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("actual", null);
        input.put("expected", null);
        input.put("iteration", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // Both default to "unknown", so they match
        assertEquals(true, result.getOutputData().get("match"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // Both default to "unknown", so they match
        assertEquals(true, result.getOutputData().get("match"));
    }

    @Test
    void mismatchWithDifferentStrings() {
        Task task = taskWith(Map.of(
                "actual", "timeout",
                "expected", "processed",
                "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("match"));
        assertEquals("timeout", result.getOutputData().get("actual"));
        assertEquals("processed", result.getOutputData().get("expected"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
