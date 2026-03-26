package datamasking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DmSelectStrategyWorkerTest {

    private final DmSelectStrategyWorker worker = new DmSelectStrategyWorker();

    @Test
    void taskDefName() {
        assertEquals("dm_select_strategy", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("purpose", "analytics"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsStrategyFlag() {
        Task task = taskWith(Map.of("purpose", "reporting"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("select_strategy"));
    }

    @Test
    void outputContainsProcessedFlag() {
        Task task = taskWith(Map.of("purpose", "analytics"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesNullPurpose() {
        Map<String, Object> input = new HashMap<>();
        input.put("purpose", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputIsDeterministic() {
        Task task = taskWith(Map.of("purpose", "dev"));
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);

        assertEquals(r1.getOutputData().get("select_strategy"), r2.getOutputData().get("select_strategy"));
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
