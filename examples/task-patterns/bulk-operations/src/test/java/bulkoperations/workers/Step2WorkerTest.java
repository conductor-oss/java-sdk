package bulkoperations.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Step2WorkerTest {

    private final Step2Worker worker = new Step2Worker();

    @Test
    void taskDefName() {
        assertEquals("bulk_step2", worker.getTaskDefName());
    }

    @Test
    void finalizesWithData() {
        Task task = taskWith(Map.of("data", "batch-42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-batch-42", result.getOutputData().get("result"));
    }

    @Test
    void finalizesWithNumericBatch() {
        Task task = taskWith(Map.of("data", "batch-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-batch-1", result.getOutputData().get("result"));
    }

    @Test
    void defaultsDataWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-unknown", result.getOutputData().get("result"));
    }

    @Test
    void defaultsDataWhenBlank() {
        Task task = taskWith(Map.of("data", "  "));
        TaskResult result = worker.execute(task);

        assertEquals("done-unknown", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResult() {
        Task task = taskWith(Map.of("data", "batch-5"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertEquals("done-batch-5", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
