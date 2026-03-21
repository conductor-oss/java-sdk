package exceptionhandling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AutoProcessWorkerTest {

    @Test
    void taskDefName() {
        AutoProcessWorker worker = new AutoProcessWorker();
        assertEquals("eh_auto_process", worker.getTaskDefName());
    }

    @Test
    void returnsProcessedTrue() {
        AutoProcessWorker worker = new AutoProcessWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void alwaysCompletes() {
        AutoProcessWorker worker = new AutoProcessWorker();
        Task task = taskWith(Map.of("extra", "data"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("processed"));
    }

    @Test
    void outputContainsProcessedKey() {
        AutoProcessWorker worker = new AutoProcessWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("processed"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
