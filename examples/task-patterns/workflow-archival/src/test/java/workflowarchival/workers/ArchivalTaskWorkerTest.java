package workflowarchival.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArchivalTaskWorkerTest {

    private final ArchivalTaskWorker worker = new ArchivalTaskWorker();

    @Test
    void taskDefName() {
        assertEquals("arch_task", worker.getTaskDefName());
    }

    @Test
    void returnsDoneTrue() {
        Task task = taskWith(Map.of("batch", "batch_1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void handlesBatchInput() {
        Task task = taskWith(Map.of("batch", "batch_42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void handlesNullBatch() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void handlesIntegerBatch() {
        Task task = taskWith(Map.of("batch", 7));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void outputContainsDoneKey() {
        Task task = taskWith(Map.of("batch", "test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("done"));
        assertInstanceOf(Boolean.class, result.getOutputData().get("done"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
