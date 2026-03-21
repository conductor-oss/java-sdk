package forkindowhile.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessBatchWorkerTest {

    private final ProcessBatchWorker worker = new ProcessBatchWorker();

    @Test
    void taskDefName() {
        assertEquals("fl_process_batch", worker.getTaskDefName());
    }

    @Test
    void processesBatchAtIterationZero() {
        Task task = taskWith(Map.of("iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("batchId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void processesBatchAtIterationTwo() {
        Task task = taskWith(Map.of("iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("batchId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void processesBatchAtIterationFive() {
        Task task = taskWith(Map.of("iteration", 5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("batchId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void defaultsIterationWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("batchId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void defaultsIterationWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("iteration", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("batchId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesIterationAsString() {
        Task task = taskWith(Map.of("iteration", "not-a-number"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("batchId"));
    }

    @Test
    void outputIsDeterministic() {
        Task task1 = taskWith(Map.of("iteration", 2));
        Task task2 = taskWith(Map.of("iteration", 2));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("batchId"), result2.getOutputData().get("batchId"));
        assertEquals(result1.getOutputData().get("processed"), result2.getOutputData().get("processed"));
    }

    @Test
    void simulateThreeIterations() {
        for (int i = 0; i < 3; i++) {
            Task task = taskWith(Map.of("iteration", i));
            TaskResult result = worker.execute(task);

            assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
            assertEquals(i + 1, result.getOutputData().get("batchId"));
            assertEquals(true, result.getOutputData().get("processed"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
