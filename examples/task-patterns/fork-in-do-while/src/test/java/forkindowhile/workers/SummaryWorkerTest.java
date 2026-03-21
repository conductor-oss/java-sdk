package forkindowhile.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SummaryWorkerTest {

    private final SummaryWorker worker = new SummaryWorker();

    @Test
    void taskDefName() {
        assertEquals("fl_summary", worker.getTaskDefName());
    }

    @Test
    void summarizesThreeIterations() {
        Task task = taskWith(Map.of("iterations", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("3 batches done", result.getOutputData().get("summary"));
    }

    @Test
    void summarizesSingleIteration() {
        Task task = taskWith(Map.of("iterations", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("1 batches done", result.getOutputData().get("summary"));
    }

    @Test
    void summarizesZeroIterations() {
        Task task = taskWith(Map.of("iterations", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("0 batches done", result.getOutputData().get("summary"));
    }

    @Test
    void defaultsWhenIterationsMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("0 batches done", result.getOutputData().get("summary"));
    }

    @Test
    void defaultsWhenIterationsNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("iterations", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("0 batches done", result.getOutputData().get("summary"));
    }

    @Test
    void handlesLargeIterationCount() {
        Task task = taskWith(Map.of("iterations", 1000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("1000 batches done", result.getOutputData().get("summary"));
    }

    @Test
    void outputIsDeterministic() {
        Task task1 = taskWith(Map.of("iterations", 3));
        Task task2 = taskWith(Map.of("iterations", 3));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("summary"), result2.getOutputData().get("summary"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
