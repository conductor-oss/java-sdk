package atleastonce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AloAcknowledgeWorkerTest {

    private final AloAcknowledgeWorker worker = new AloAcknowledgeWorker();

    @Test
    void taskDefName() {
        assertEquals("alo_acknowledge", worker.getTaskDefName());
    }

    @Test
    void executesSuccessfully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("test", "value")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsExpectedKey() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("test", "value")));
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("acknowledged"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }
}