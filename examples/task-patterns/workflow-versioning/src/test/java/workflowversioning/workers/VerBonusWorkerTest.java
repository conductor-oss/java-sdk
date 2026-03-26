package workflowversioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerBonusWorkerTest {

    private final VerBonusWorker worker = new VerBonusWorker();

    @Test
    void taskDefName() {
        assertEquals("ver_bonus", worker.getTaskDefName());
    }

    @Test
    void addsBonus() {
        Task task = taskWith(Map.of("baseResult", 100));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(110, result.getOutputData().get("result"));
    }

    @Test
    void addsBonusToZero() {
        Task task = taskWith(Map.of("baseResult", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(10, result.getOutputData().get("result"));
    }

    @Test
    void addsBonusToNegativeValue() {
        Task task = taskWith(Map.of("baseResult", -5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5, result.getOutputData().get("result"));
    }

    @Test
    void defaultsToZeroWhenBaseResultMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(10, result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
