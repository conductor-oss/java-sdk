package workflowpatterns.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WpLoopIterationWorkerTest {

    private final WpLoopIterationWorker worker = new WpLoopIterationWorker();

    @Test
    void taskDefName() {
        assertEquals("wp_loop_iteration", worker.getTaskDefName());
    }

    @Test
    void executesIterationSuccessfully() {
        Task task = taskWith(Map.of("iteration", 1, "merged", "merged_a_b"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("iteration"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesZeroIteration() {
        Task task = taskWith(Map.of("iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("iteration"));
    }

    @Test
    void handlesNullIteration() {
        Map<String, Object> input = new HashMap<>();
        input.put("iteration", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("iteration"));
    }

    @Test
    void handlesMissingIteration() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("iteration"));
    }

    @Test
    void processedIsAlwaysTrue() {
        Task task = taskWith(Map.of("iteration", 5));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesLargeIterationNumber() {
        Task task = taskWith(Map.of("iteration", 999));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(999, result.getOutputData().get("iteration"));
    }

    @Test
    void outputContainsBothKeys() {
        Task task = taskWith(Map.of("iteration", 2));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("iteration"));
        assertTrue(result.getOutputData().containsKey("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
