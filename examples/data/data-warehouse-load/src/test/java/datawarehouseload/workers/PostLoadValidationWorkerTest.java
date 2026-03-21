package datawarehouseload.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PostLoadValidationWorkerTest {

    private final PostLoadValidationWorker worker = new PostLoadValidationWorker();

    @Test
    void taskDefName() {
        assertEquals("wh_post_load_validation", worker.getTaskDefName());
    }

    @Test
    void validatesSuccessfully() {
        Task task = taskWith(Map.of("targetTable", "fact_revenue", "expectedCount", 5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("passed"));
        assertEquals(true, result.getOutputData().get("rowCountMatch"));
    }

    @Test
    void expectedCountMatchesOutput() {
        Task task = taskWith(Map.of("targetTable", "fact_revenue", "expectedCount", 15));
        TaskResult result = worker.execute(task);

        assertEquals(15, result.getOutputData().get("expectedCount"));
    }

    @Test
    void handlesZeroExpectedCount() {
        Task task = taskWith(Map.of("targetTable", "fact_empty", "expectedCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("expectedCount"));
    }

    @Test
    void handlesMissingExpectedCount() {
        Task task = taskWith(Map.of("targetTable", "fact_test"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("expectedCount"));
    }

    @Test
    void alwaysReturnsPassedTrue() {
        Task task = taskWith(Map.of("expectedCount", 100));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("passed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
