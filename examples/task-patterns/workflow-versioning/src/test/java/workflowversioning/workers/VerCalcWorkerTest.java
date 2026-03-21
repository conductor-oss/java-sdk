package workflowversioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerCalcWorkerTest {

    private final VerCalcWorker worker = new VerCalcWorker();

    @Test
    void taskDefName() {
        assertEquals("ver_calc", worker.getTaskDefName());
    }

    @Test
    void calculatesDoubleOfValue() {
        Task task = taskWith(Map.of("value", 50));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(100, result.getOutputData().get("result"));
    }

    @Test
    void calculatesDoubleOfZero() {
        Task task = taskWith(Map.of("value", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("result"));
    }

    @Test
    void calculatesDoubleOfNegativeValue() {
        Task task = taskWith(Map.of("value", -10));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(-20, result.getOutputData().get("result"));
    }

    @Test
    void defaultsToZeroWhenValueMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("result"));
    }

    @Test
    void handlesLargeValue() {
        Task task = taskWith(Map.of("value", 1000000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2000000, result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
